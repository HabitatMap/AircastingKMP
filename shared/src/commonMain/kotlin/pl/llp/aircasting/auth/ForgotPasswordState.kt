package pl.llp.aircasting.auth

data class ForgotPasswordState(
  val email: String = "",
  val status: Status = Status.Editing,
) {
  enum class Status { Editing, Sending, Sent, Failed }

  val canSubmit: Boolean get() = email.isNotBlank() && status != Status.Sending
}
