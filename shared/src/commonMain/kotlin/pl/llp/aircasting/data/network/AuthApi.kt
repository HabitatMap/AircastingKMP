package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException

sealed interface AuthResponse {
  data class Authenticated(val user: UserDto) : AuthResponse
  /** 401 — wrong credentials. Sign-in only. */
  data object Rejected : AuthResponse
  /** 422 — the backend refused the new account, per field. Sign-up only. */
  data class Invalid(val errors: AccountErrorsDto) : AuthResponse
  data object Failed : AuthResponse
}

class AuthApi(private val client: HttpClient) {
  suspend fun signIn(login: String, password: String): AuthResponse =
    client.get("/api/user.json") {
      expectSuccess = false
      basicAuth(login, password)
    }.toAuthResponse()

  suspend fun signUp(email: String, username: String, password: String): AuthResponse =
    client.post("/api/user.json") {
      expectSuccess = false
      contentType(ContentType.Application.Json)
      setBody(CreateAccountBody(CreateAccountParams(username, password, email)))
    }.toAuthResponse()
}

private suspend fun HttpResponse.toAuthResponse(): AuthResponse = try {
  when {
    status.isSuccess() -> AuthResponse.Authenticated(body())
    status == HttpStatusCode.Unauthorized -> AuthResponse.Rejected
    status == HttpStatusCode.UnprocessableEntity -> AuthResponse.Invalid(body())
    else -> AuthResponse.Failed
  }
} catch (malformed: SerializationException) {
  AuthResponse.Failed          // 2xx with a payload we can't read is not a login
} catch (notJson: NoTransformationFoundException) {
  AuthResponse.Failed          // e.g. an HTML error page from a proxy
}