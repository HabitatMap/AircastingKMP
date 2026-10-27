package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AccountApi(private val client: HttpClient) {

  suspend fun user(token: String): UserDto =
    client.get(ApiEndpoints.User) { aircastingBearerAuth(token) }.body()

  suspend fun requestPasswordReset(login: String) {
    client.post(ApiEndpoints.PasswordReset) {
      contentType(ContentType.Application.Json)
      setBody(PasswordResetBody(PasswordResetBody.Login(login)))
    }
  }
  suspend fun sendAccountDeletionCode(token: String) {
    client.post(ApiEndpoints.DeleteAccountSendCode) { aircastingBearerAuth(token) }
  }

  suspend fun confirmAccountDeletion(token: String, code: String) {
    client.post("/api/user/delete_account_confirm") {
      aircastingBearerAuth(token)
      contentType(ContentType.Application.Json)
      setBody(DeleteAccountCodeBody(code))
    }
  }
  suspend fun updateUser(token: String, params: UpdateAccountParams): AccountUpdateResponse {
    val response = client.patch(ApiEndpoints.User) {
      expectSuccess = false
      aircastingBearerAuth(token)
      contentType(ContentType.Application.Json)
      setBody(UpdateAccountBody(params))
    }
    return when {
      response.status.isSuccess() -> AccountUpdateResponse.Updated(response.body())
      response.status == HttpStatusCode.UnprocessableEntity -> AccountUpdateResponse.Invalid(response.body())
      else -> AccountUpdateResponse.Failed(response.status)
    }
  }
}

sealed interface AccountUpdateResponse {
  data class Updated(val user: UserDto) : AccountUpdateResponse
  /** 422 — validation error, wrong current_password, or nothing to change. */
  data class Invalid(val error: ApiErrorDto) : AccountUpdateResponse
  data class Failed(val status: HttpStatusCode) : AccountUpdateResponse
}