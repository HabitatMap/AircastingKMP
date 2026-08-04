package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
