package pl.llp.aircasting.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsScaffold

@Composable
fun ForgotPasswordScreen(
  state: ForgotPasswordState,
  onEmailChange: (String) -> Unit,
  onSubmit: () -> Unit,
  onBack: () -> Unit,
) {
  val strings = LocalStrings.current
  // The design's app bar (270:11348) carries the back arrow only, so the title is blank —
  // reusing SettingsScaffold keeps the arrow's placement and tint identical to every other
  // pushed screen.
  SettingsScaffold(title = "", onBack = onBack) { padding ->
    Column(
      Modifier.padding(padding).padding(horizontal = 24.dp).padding(top = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AuthHeader(
        title = strings.forgotPasswordTitle,
        subtitle = strings.forgotPasswordBody,
        titleStyle = MaterialTheme.typography.headlineLargeEmphasized,
      )
      Spacer(Modifier.height(37.dp))
      AuthTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = strings.forgotPasswordEmailLabel,
        keyboardType = KeyboardType.Email,
      )
      StatusMessage(state.status)
      Spacer(Modifier.height(48.dp))
      OutlinedButton(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = state.canSubmit,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primaryContainer),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        if (state.status == ForgotPasswordState.Status.Sending) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        } else {
          Text(strings.forgotPasswordSubmit, style = MaterialTheme.typography.titleMedium)
        }
      }
      Spacer(Modifier.height(24.dp))
      Text(
        strings.forgotPasswordBackToSignIn,
        modifier = Modifier.clickable(onClick = onBack),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}

@Composable
private fun StatusMessage(status: ForgotPasswordState.Status) {
  val strings = LocalStrings.current
  val (text, color) = when (status) {
    ForgotPasswordState.Status.Sent ->
      strings.forgotPasswordSent to MaterialTheme.colorScheme.onPrimaryContainer
    ForgotPasswordState.Status.Failed ->
      strings.forgotPasswordFailed to MaterialTheme.colorScheme.error
    else -> return
  }
  Spacer(Modifier.height(16.dp))
  Text(
    text,
    modifier = Modifier.fillMaxWidth(),
    style = MaterialTheme.typography.bodyMedium,
    color = color,
    textAlign = TextAlign.Center,
  )
}