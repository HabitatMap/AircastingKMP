package pl.llp.aircasting.settings.account

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import com.russhwolf.settings.MapSettings
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.AuthState
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkAccountRepositoryTest {

  @Test
  fun profile_authenticates_with_the_stored_token_and_maps_username_to_name() = runTest {
    var auth: String? = null
    val repo = repository(USER_JSON, token = "tok-123") { auth = it.headers[HttpHeaders.Authorization] }

    val profile = repo.profile()

    assertEquals(AccountProfile(name = "adam.smith", email = "adam.smith.1989@mail.com"), profile)
    assertNotNull(auth)
    assertEquals("tok-123:X", Base64.decode(auth!!.removePrefix("Basic ")).decodeToString())
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
    assertEquals(AuthState.SignedIn("tok-123"), session.state.value)
  }

  @Test
  fun deletion_confirm_clears_the_token_on_success() = runTest {
    val session = signedIn()
    val repo = repository("""{"error":null,"message":"deleted"}""", session = session)

    repo.confirmAccountDeletion("1234")

    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun an_unauthorised_profile_request_fails_loudly() = runTest {
    val repo = repository(
      """{"error":"You need to sign in or sign up before continuing."}""",
      status = HttpStatusCode.Unauthorized,
      token = "stale",
    )
    assertFailsWith<ClientRequestException> { repo.profile() }
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

  /** A fresh session, signed in with [token] unless it is null. */
  private fun signedIn(token: String? = "tok-123") =
    StoredAuthSession(MapSettings()).apply { token?.let(::start) }
  private fun io.ktor.http.content.OutgoingContent.toByteReadPacketString(): String =
    (this as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()

  private companion object {
    const val EMPTY_JSON = "{}"
    val USER_JSON = """
      {
        "email": "adam.smith.1989@mail.com",
        "username": "adam.smith",
        "authentication_token": "tok-123",
        "session_stopped_alert": true
      }
    """.trimIndent()
  }
}