package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountBody(val user: CreateAccountParams)

@Serializable
data class CreateAccountParams(
  val username: String,
  val password: String,
  val email: String,
  @SerialName("send_emails") val sendEmails: Boolean = true,
  @SerialName("session_stopped_alert") val sessionStoppedAlert: Boolean = true,
)

@Serializable
data class AccountErrorsDto(
  val email: List<String> = emptyList(),
  val username: List<String> = emptyList(),
  val password: List<String> = emptyList(),
)