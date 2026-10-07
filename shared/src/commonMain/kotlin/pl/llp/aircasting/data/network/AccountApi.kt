package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AccountApi(private val client: HttpClient) {

  suspend fun user(token: String): UserDto =
    client.get(ApiEndpoints.User) { basicAuth(token, PasswordPlaceholder) }.body()

  suspend fun requestPasswordReset(login: String) {
    client.post(ApiEndpoints.PasswordReset) {
      contentType(ContentType.Application.Json)
      setBody(PasswordResetBody(PasswordResetBody.Login(login)))
    }
  }
  suspend fun sendAccountDeletionCode(token: String) {
    client.post(ApiEndpoints.DeleteAccountSendCode) { basicAuth(token, PasswordPlaceholder) }
  }

  suspend fun confirmAccountDeletion(token: String, code: String) {
    client.post("/api/user/delete_account_confirm") {
      basicAuth(token, PasswordPlaceholder)
      contentType(ContentType.Application.Json)
      setBody(DeleteAccountCodeBody(code))
    }
  }
  private companion object { const val PasswordPlaceholder = "X" }
}