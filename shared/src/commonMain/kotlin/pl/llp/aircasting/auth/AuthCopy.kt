package pl.llp.aircasting.auth

import pl.llp.aircasting.i18n.Strings

fun Strings.message(failure: AuthFailure): String = when (failure) {
  AuthFailure.InvalidCredentials -> authInvalidCredentials
  AuthFailure.Unexpected, is AuthFailure.Invalid -> authUnexpectedError
}
