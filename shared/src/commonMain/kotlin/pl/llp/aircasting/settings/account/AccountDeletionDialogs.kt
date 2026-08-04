package pl.llp.aircasting.settings.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings

@Composable
internal fun DeleteAccountConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val strings = LocalStrings.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.deleteAccountConfirmTitle) },
    text = { Text(strings.deleteAccountConfirmBody) },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        // Destructive intent gets the error color; the neutral action stays default so the
        // dangerous one is never the visually easy choice.
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) { Text(strings.deleteAccountSendCode) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

@Composable
internal fun DeleteAccountCodeDialog(
  email: String,
  rejected: Boolean,
  onSubmit: (String) -> Unit,
  onResend: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  var code by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.deleteAccountCodeTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(strings.deleteAccountCodeBody(email))
        OutlinedTextField(
          value = code,
          // Filter at the source: the backend code is exactly 4 digits, so anything else is a
          // guaranteed 401 and a wasted round trip.
          onValueChange = { input -> if (input.length <= CodeLength && input.all(Char::isDigit)) code = input },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(strings.deleteAccountCodeLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          isError = rejected,
          supportingText = if (rejected) {
            { Text(strings.deleteAccountCodeInvalid, color = MaterialTheme.colorScheme.error) }
          } else null,
          singleLine = true,
        )
        TextButton(onClick = { code = ""; onResend() }) { Text(strings.deleteAccountResend) }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSubmit(code) },
        enabled = code.length == CodeLength,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) { Text(strings.accountDeleteAccount) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

private const val CodeLength = 4