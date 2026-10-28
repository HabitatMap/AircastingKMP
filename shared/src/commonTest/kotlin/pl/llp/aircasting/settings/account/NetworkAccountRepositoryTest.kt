package pl.llp.aircasting.settings.account

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.AuthState
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class NetworkAccountRepositoryTest {

  @Test
  fun profile_authenticates_with_the_stored_token_and_maps_username_to_name() = runTest {
    var auth: String? = null
    val repo = repository(USER_JSON, token = "tok-123") { auth = it.headers[HttpHeaders.Authorization] }

    val profile = repo.profile()

    assertEquals(AccountProfile(name = "adam.smith", email = "adam.smith.1989@mail.com"), profile)
    assertNotNull(auth)
    assertEquals("Bearer tok-123", auth)
  }

  @Test
  fun sign_out_clears_the_token() = runTest {
    val session = signedIn()
    val repo = repository(USER_JSON, session = session)

    repo.signOut()

    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun password_reset_posts_the_login_wrapped_in_user() = runTest {
    var body: String? = null
    var path: String? = null
    val repo = repository(EMPTY_JSON, token = "tok-123") { request ->
      path = request.url.encodedPath
      body = request.body.toByteReadPacketString()
    }

    repo.requestPasswordReset("adam.smith.1989@mail.com")

    assertEquals("/users/password.json", path)
    assertEquals("""{"user":{"login":"adam.smith.1989@mail.com"}}""", body)
  }

  @Test
  fun deletion_confirm_keeps_the_token_when_the_code_is_rejected() = runTest {
    val session = signedIn()
    val repo = repository(
      """{"error":"Invalid or expired confirmation code."}""",
      status = HttpStatusCode.Unauthorized,
      session = session,
    )

    assertFailsWith<ClientRequestException> { repo.confirmAccountDeletion("0000") }
    assertEquals(AuthState.SignedIn("tok-123", Stored), session.state.value)
  }

  @Test
  fun deletion_confirm_clears_the_token_on_success() = runTest {
    val session = signedIn()
    val repo = repository("""{"error":null,"message":"deleted"}""", session = session)

    repo.confirmAccountDeletion("1234")

    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun an_unauthorised_profile_request_ends_the_session() = runTest {
    val session = signedIn()
    val repo = repository(UNAUTHORIZED_JSON, status = HttpStatusCode.Unauthorized, session = session)

    assertFailsWith<SessionExpired> { repo.profile() }

    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun a_401_for_a_previous_account_does_not_end_the_current_session() = runTest {
    val session = signedIn()
    val bob = AccountProfile(name = "bob", email = "bob@example.com")
    // The request left with tok-123; by the time its 401 arrives, bob is signed in.
    val repo = repository(UNAUTHORIZED_JSON, status = HttpStatusCode.Unauthorized, session = session) {
      session.end(); session.start("tok-456", bob)
    }

    assertFails { repo.profile() }

    assertEquals(AuthState.SignedIn("tok-456", bob), session.state.value)
  }

  private fun repository(
    responseJson: String = EMPTY_JSON,
    status: HttpStatusCode = HttpStatusCode.OK,
    token: String? = "tok-123",
    session: AuthSession = signedIn(token),
    onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
  ): AccountRepository {
    val engine = MockEngine { request ->
      onRequest(request)
      respond(responseJson, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return NetworkAccountRepository(AccountApi(createAircastingHttpClient(engine)), session)
  }


  @Test
  fun update_profile_patches_only_the_changed_fields_with_bearer_auth() = runTest {
    var request: io.ktor.client.request.HttpRequestData? = null
    val repo = repository(USER_JSON, token = "tok-123") { request = it }

    val profile = repo.updateProfile(email = "adam.smith.1989@mail.com")

    assertEquals(io.ktor.http.HttpMethod.Patch, request!!.method)
    assertEquals("/api/user.json", request!!.url.encodedPath)
    assertEquals("Bearer tok-123", request!!.headers[HttpHeaders.Authorization])
    // Nulls must be omitted, not sent as null — the backend treats every sent key as a change.
    assertEquals("""{"user":{"email":"adam.smith.1989@mail.com"}}""", request!!.body.toByteReadPacketString())
    assertEquals(AccountProfile(name = "adam.smith", email = "adam.smith.1989@mail.com"), profile)
  }

  @Test
  fun change_password_sends_the_confirmation_and_the_current_password() = runTest {
    var body: String? = null
    val repo = repository(USER_JSON) { body = it.body.toByteReadPacketString() }

    repo.changePassword(current = "old-pass", new = "new-pass")

    assertEquals(
      """{"user":{"password":"new-pass","password_confirmation":"new-pass","current_password":"old-pass"}}""",
      body,
    )
  }

  @Test
  fun a_rejected_update_keeps_the_session() = runTest {
    val session = signedIn()
    val repo = repository(SPEC_422_JSON, status = HttpStatusCode.UnprocessableEntity, session = session)

    assertFailsWith<AccountUpdateRejected> { repo.changePassword(current = "wrong", new = "short") }
    assertEquals(AuthState.SignedIn("tok-123", Stored), session.state.value)
  }

  @Test
  fun a_422_surfaces_each_field_error_with_its_code_and_params() = runTest {
    val repo = repository(SPEC_422_JSON, status = HttpStatusCode.UnprocessableEntity)

    val rejected = assertFailsWith<AccountUpdateRejected> { repo.changePassword(current = "wrong", new = "short") }

    assertEquals("Account could not be updated", rejected.message)
    assertEquals(listOf(FieldError("invalid", "is invalid")), rejected.fields["current_password"])
    assertEquals(
      listOf(FieldError("too_short", "is too short (minimum is 8 characters)", mapOf("min" to "8"))),
      rejected.fields["password"],
    )
  }

  @Test
  fun a_server_error_is_not_mistaken_for_a_rejection() = runTest {
    val repo = repository("<html>oops</html>", status = HttpStatusCode.InternalServerError)

    assertFailsWith<IllegalStateException> { repo.updateProfile(email = "adam@new-mail.com") }
  }

  @Test
  fun the_stored_profile_is_read_without_a_request() = runTest {
    val repo = repository(USER_JSON) { fail("expected no request, got ${it.url}") }

    assertEquals(Stored, repo.storedProfile)
  }

  @Test
  fun a_fetched_profile_replaces_the_stored_one() = runTest {
    val session = signedIn()
    val repo = repository(USER_JSON, session = session)

    repo.profile()

    assertEquals(AuthState.SignedIn("tok-123", Adam), session.state.value)
  }

  @Test
  fun a_failed_fetch_keeps_the_stored_profile() = runTest {
    val session = signedIn()
    val repo = repository("<html>oops</html>", status = HttpStatusCode.InternalServerError, session = session)

    assertFails { repo.profile() }

    assertEquals(AuthState.SignedIn("tok-123", Stored), session.state.value)
  }

  @Test
  fun an_updated_profile_replaces_the_stored_one() = runTest {
    val session = signedIn()
    val repo = repository(USER_JSON, session = session)

    repo.updateProfile(email = "adam.smith.1989@mail.com")

    assertEquals(AuthState.SignedIn("tok-123", Adam), session.state.value)
  }

  @Test
  fun a_rejected_update_keeps_the_stored_profile() = runTest {
    val session = signedIn()
    val repo = repository(SPEC_422_JSON, status = HttpStatusCode.UnprocessableEntity, session = session)

    assertFailsWith<AccountUpdateRejected> { repo.updateProfile(email = "taken@mail.com") }

    assertEquals(AuthState.SignedIn("tok-123", Stored), session.state.value)
  }

  @Test
  fun a_profile_fetched_before_an_account_switch_is_not_stored() = runTest {
    val session = signedIn()
    val bob = AccountProfile(name = "bob", email = "bob@example.com")
    // Runs while the request is in flight: it already left with tok-123.
    val repo = repository(USER_JSON, session = session) { session.end(); session.start("tok-456", bob) }

    repo.profile()

    assertEquals(AuthState.SignedIn("tok-456", bob), session.state.value)
  }

  @Test
  fun an_update_answered_after_an_account_switch_is_not_stored() = runTest {
    val session = signedIn()
    val bob = AccountProfile(name = "bob", email = "bob@example.com")
    val repo = repository(USER_JSON, session = session) { session.end(); session.start("tok-456", bob) }

    repo.updateProfile(email = "adam.smith.1989@mail.com")

    assertEquals(AuthState.SignedIn("tok-456", bob), session.state.value)
  }

  private fun io.ktor.http.content.OutgoingContent.toByteReadPacketString(): String =
    (this as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()

  /** A fresh session, signed in with [token] and [Stored] unless the token is null. */
  private fun signedIn(token: String? = "tok-123") =
    StoredAuthSession(MapSettings()).apply { token?.let { start(it, Stored) } }


  private companion object {
    /** What the session holds before any request: deliberately different from [USER_JSON]. */
    val Stored = AccountProfile(name = "u", email = "u@example.com")
    val Adam = AccountProfile(name = "adam.smith", email = "adam.smith.1989@mail.com")
    const val EMPTY_JSON = "{}"
    val SPEC_422_JSON = """
      {
        "error_code": "validation_error",
        "message": "Account could not be updated",
        "fields": {
          "current_password": [{ "code": "invalid", "message": "is invalid" }],
          "password": [{ "code": "too_short", "message": "is too short (minimum is 8 characters)", "params": { "min": 8 } }]
        }
      }
    """.trimIndent()
    val USER_JSON = """
      {
        "email": "adam.smith.1989@mail.com",
        "username": "adam.smith",
        "authentication_token": "tok-123",
        "session_stopped_alert": true
      }
    """.trimIndent()
    val UNAUTHORIZED_JSON = """{"error_code":"unauthorized","message":"Missing or invalid token"}"""

  }
}