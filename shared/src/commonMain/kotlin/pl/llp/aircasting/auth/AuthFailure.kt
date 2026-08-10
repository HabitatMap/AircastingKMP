package pl.llp.aircasting.auth

sealed class AuthFailure : Exception() {
  /** 401 on sign-in. */
  data object InvalidCredentials : AuthFailure()
  /** 422 on sign-up: the first backend message per field, `null` where that field was fine. */
  data class Invalid(
    val email: String?,
    val username: String?,
    val password: String?,
  ) : AuthFailure()
  /** Offline, 5xx, or an unreadable payload — nothing the user can fix by editing the form. */
  data object Unexpected : AuthFailure()
}