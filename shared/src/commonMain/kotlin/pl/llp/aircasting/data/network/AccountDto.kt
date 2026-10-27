package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UserDto(
  val email: String,
  val username: String,
  @SerialName("authentication_token") val authenticationToken: String,
)

@Serializable
data class PasswordResetBody(val user: Login) {
  @Serializable
  data class Login(val login: String)
}

@Serializable
data class DeleteAccountCodeBody(val code: String)

@Serializable
data class UpdateAccountBody(val user: UpdateAccountParams)

@Serializable
data class UpdateAccountParams(
  val username: String? = null,
  val email: String? = null,
  val password: String? = null,
  @SerialName("password_confirmation") val passwordConfirmation: String? = null,
  @SerialName("current_password") val currentPassword: String? = null,
)
@Serializable
data class ApiErrorDto(
  @SerialName("error_code") val errorCode: String,
  val message: String,
  val fields: Map<String, List<FieldErrorDto>> = emptyMap(),
)
@Serializable
data class FieldErrorDto(val code: String, val message: String, val params: JsonObject? = null)