package pl.llp.aircasting.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsSheet

@Composable
fun AuthSheet(state: AuthFormState, actions: AuthFormActions, onDismiss: () -> Unit) {
  val strings = LocalStrings.current
  SettingsSheet(
    title = when (state.mode) {
      AuthMode.SignIn -> strings.authTabSignIn
      AuthMode.SignUp -> strings.authTabSignUp
    },
    onDismiss = onDismiss,
    onConfirm = actions.onSubmit,
    confirmEnabled = state.canSubmit,
  ) {
  }

  Column(Modifier.padding(horizontal = 16.dp)) {
    AuthHeader(
      title = when (state.mode) {
        AuthMode.SignIn -> strings.authWelcomeTitle
        AuthMode.SignUp -> strings.authTabSignUp
      },
      subtitle = when (state.mode) {
        AuthMode.SignIn -> strings.authSignInSheetSubtitle
        AuthMode.SignUp -> strings.authSignUpSheetSubtitle
      },
      titleStyle = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(40.dp))
    AuthForm(state, actions)
    Spacer(Modifier.height(32.dp))
  }
}
