package pl.llp.aircasting.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings

@Stable
class AuthFormActions(
  val onLoginChange: (String) -> Unit,
  val onEmailChange: (String) -> Unit,
  val onUsernameChange: (String) -> Unit,
  val onPasswordChange: (String) -> Unit,
  val onTogglePassword: () -> Unit,
  val onSwitchMode: (AuthMode) -> Unit,
  val onForgotPassword: () -> Unit,
  val onSubmit: () -> Unit,
)

@Composable
fun AuthForm(state: AuthFormState, actions: AuthFormActions, modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  Column(modifier.fillMaxWidth()) {
    when (state.mode) {
      // Sign-in takes one identifier; the backend resolves either an email or a profile name.
      AuthMode.SignIn -> AuthTextField(
        value = state.login,
        onValueChange = actions.onLoginChange,
        label = strings.authLoginLabel,
        keyboardType = KeyboardType.Email,
      )
      AuthMode.SignUp -> {
        AuthTextField(
          value = state.email,
          onValueChange = actions.onEmailChange,
          label = strings.authEmailLabel,
          error = state.emailError,
          keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(24.dp))
        AuthTextField(
          value = state.username,
          onValueChange = actions.onUsernameChange,
          label = strings.authUsernameLabel,
          error = state.usernameError,
        )
      }
    }
    Spacer(Modifier.height(24.dp))
    AuthPasswordField(
      value = state.password,
      onValueChange = actions.onPasswordChange,
      label = when (state.mode) {
        AuthMode.SignIn -> strings.authPasswordLabel
        AuthMode.SignUp -> strings.authNewPasswordLabel
      },
      revealed = state.passwordVisible,
      onToggleReveal = actions.onTogglePassword,
      onSubmit = actions.onSubmit,
      error = state.passwordError,
    )
    if (state.mode == AuthMode.SignIn) {
      Spacer(Modifier.height(16.dp))
      Text(
        strings.authForgotPassword,
        modifier = Modifier
          .align(Alignment.End)
          .clickable(onClick = actions.onForgotPassword),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }

    state.formError?.let { failure ->
      Spacer(Modifier.height(16.dp))
      Text(
        strings.message(failure),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(48.dp))
    SubmitButton(state, actions.onSubmit)
    Spacer(Modifier.height(24.dp))
    ModeSwitchPrompt(state.mode, actions.onSwitchMode)
  }
}

@Composable
internal fun AuthHeader(title: String, subtitle: String, titleStyle: TextStyle) {
  Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      title,
      style = titleStyle,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      subtitle,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun SubmitButton(state: AuthFormState, onClick: () -> Unit) {
  val strings = LocalStrings.current
  // Same recipe as SettingsAccountScreen's SignOutButton — the design uses one outlined
  // button everywhere. (Figma spec's #003547 vs our onPrimaryContainer #004059 is a
  // one-shade drift in the token set; following the theme rather than the raw hex.)
  OutlinedButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(56.dp),
    enabled = state.canSubmit,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primaryContainer),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
  ) {
    if (state.submitting) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    } else {
      Text(
        when (state.mode) {
          AuthMode.SignIn -> strings.authSignInAction
          AuthMode.SignUp -> strings.authSignUpAction
        },
        style = MaterialTheme.typography.titleMedium,
      )
    }
  }
}

@Composable
private fun ModeSwitchPrompt(mode: AuthMode, onSwitch: (AuthMode) -> Unit) {
  val strings = LocalStrings.current
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      when (mode) {
        AuthMode.SignIn -> strings.authNoAccountPrompt
        AuthMode.SignUp -> strings.authHaveAccountPrompt
      },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.width(6.dp))
    Text(
      when (mode) {
        AuthMode.SignIn -> strings.authSignUpAction
        AuthMode.SignUp -> strings.authSignInAction
      },
      modifier = Modifier.clickable {
        onSwitch(if (mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn)
      },
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}