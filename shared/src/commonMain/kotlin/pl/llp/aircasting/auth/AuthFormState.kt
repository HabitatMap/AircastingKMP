package pl.llp.aircasting.auth

enum class AuthMode { SignIn, SignUp }

data class AuthFormState(
  val mode: AuthMode = AuthMode.SignIn,
  val login: String = "",
  val email: String = "",
  val username: String = "",
  val password: String = "",
  val passwordVisible: Boolean = false,
  val submitting: Boolean = false,
  val failure: AuthFailure? = null,
) {
  val canSubmit: Boolean
    get() = !submitting && when (mode) {
      AuthMode.SignIn -> login.isNotBlank() && password.isNotBlank()
      AuthMode.SignUp -> email.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

  private val fieldErrors: AuthFailure.Invalid? get() = failure as? AuthFailure.Invalid
  val emailError: String? get() = fieldErrors?.email
  val usernameError: String? get() = fieldErrors?.username
  val passwordError: String? get() = fieldErrors?.password
  val formError: AuthFailure? get() = failure?.takeIf { it !is AuthFailure.Invalid }
}