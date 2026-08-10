package pl.llp.aircasting.auth

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.data.auth.AuthState
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.AuthApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkAuthRepositoryTest {

  private fun repository(engine: MockEngine) =
    NetworkAuthRepository(AuthApi(createAircastingHttpClient(engine)), StoredAuthSession(MapSettings()))

  private fun json(body: String, status: HttpStatusCode) = MockEngine {
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
  }

  @Test
  fun sign_in_sends_the_raw_credentials_as_basic_auth_and_starts_the_session() = runTest {
    var request: HttpRequestData? = null
    val engine = MockEngine { req ->
      request = req
      respond(USER_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val repo = repository(engine)

    val result = repo.signIn("ada", "s3cret")

    assertTrue(result.isSuccess)
    assertEquals(AuthState.SignedIn("tok-123"), repo.state.value)
    assertEquals("/api/user.json", request!!.url.encodedPath)
    // Base64("ada:s3cret") — credentials, NOT Base64("token:X") like the other endpoints.
    assertEquals("Basic YWRhOnMzY3JldA==", request!!.headers[HttpHeaders.Authorization])
  }

  @Test
  fun a_401_reports_invalid_credentials_and_leaves_the_user_signed_out() = runTest {
    val repo = repository(MockEngine { respondError(HttpStatusCode.Unauthorized) })

    val result = repo.signIn("ada", "wrong")

    assertEquals(AuthFailure.InvalidCredentials, result.exceptionOrNull())
    assertEquals(AuthState.SignedOut, repo.state.value)
  }

  @Test
  fun sign_up_posts_the_nested_user_body_the_backend_expects() = runTest {
    var request: HttpRequestData? = null
    val engine = MockEngine { req ->
      request = req
      respond(USER_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val repo = repository(engine)

    val result = repo.signUp(email = "ada@example.com", username = "ada", password = "s3cret")

    assertTrue(result.isSuccess)
    assertEquals(HttpMethod.Post, request!!.method)
    // Pinned verbatim: the `user` wrapper and the two snake_case flags are easy to lose in a
    // refactor and the backend silently 422s without them.
    assertEquals(
      """{"user":{"username":"ada","password":"s3cret","email":"ada@example.com",""" +
        """"send_emails":true,"session_stopped_alert":true}}""",
      (request!!.body as TextContent).text,
    )
    assertEquals(AuthState.SignedIn("tok-123"), repo.state.value)
  }

  @Test
  fun a_422_surfaces_the_backends_per_field_messages() = runTest {
    val body = """
      {"email":["has already been taken"],"username":[],"password":["is too short"]}
    """.trimIndent()
    val repo = repository(json(body, HttpStatusCode.UnprocessableEntity))

    val result = repo.signUp("ada@example.com", "ada", "short")

    assertEquals(
      AuthFailure.Invalid(
        email = "has already been taken",
        username = null,          // empty list means this field was fine
        password = "is too short",
      ),
      result.exceptionOrNull(),
    )
    assertEquals(AuthState.SignedOut, repo.state.value)
  }

  @Test
  fun a_server_error_comes_back_as_a_failed_result_not_a_thrown_exception() = runTest {
    val repo = repository(MockEngine { respondError(HttpStatusCode.InternalServerError) })

    assertEquals(AuthFailure.Unexpected, repo.signIn("ada", "s3cret").exceptionOrNull())
  }

  @Test
  fun signing_out_ends_the_session() = runTest {
    val repo = repository(json(USER_JSON, HttpStatusCode.OK))
    repo.signIn("ada", "s3cret")

    repo.signOut()

    assertEquals(AuthState.SignedOut, repo.state.value)
  }

  private companion object {
    const val USER_JSON = """
      {"email":"ada@example.com","username":"ada",
       "authentication_token":"tok-123","session_stopped_alert":true}
    """
  }
}