package pl.llp.aircasting.settings.account

import pl.llp.aircasting.i18n.Strings

enum class AccountAction { ChangeEmail, ChangeUsername, ChangePassword }

fun Strings.label(action: AccountAction): String = when (action) {
  AccountAction.ChangeEmail -> accountChangeEmail
  AccountAction.ChangeUsername -> accountChangeUsername
  AccountAction.ChangePassword -> accountChangePassword
}