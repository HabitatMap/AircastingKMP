package pl.llp.aircasting.settings.account

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_visibility
import aircasting.shared.generated.resources.ic_visibility_off
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings

@Composable
internal fun ChangePasswordDialog(
  errors: Map<PasswordField, EditError>,
  onSave: (current: String, new: String, confirmation: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  // remember, not rememberSaveable: passwords shouldn't be written into the saved-state bundle.
  val values = remember { mutableStateMapOf<PasswordField, String>() }
  var revealed by remember { mutableStateOf(false) }
  val complete = PasswordField.entries.all { !values[it].isNullOrEmpty() }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.accountChangePassword) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PasswordField.entries.forEach { field ->
          val error = errors[field]
          OutlinedTextField(
            value = values[field].orEmpty(),
            onValueChange = { values[field] = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.label(field)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { revealed = !revealed }) {
                Icon(
                  painterResource(if (revealed) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility),
                  contentDescription = if (revealed) strings.authHidePassword else strings.authShowPassword,
                )
              }
            },
            isError = error != null,
            supportingText = error?.let { { Text(strings.describe(field, it), color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSave(
            values.getValue(PasswordField.Current),
            values.getValue(PasswordField.New),
            values.getValue(PasswordField.Confirmation),
          )
        },
        enabled = complete,
      ) { Text(strings.accountSave) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

/**
 * The backend answers a wrong current password with code `invalid` and an English message. On
 * any other field `invalid` means "no specific code" and must fall back to the message, but on
 * this one it can only mean the password was wrong, so it gets a localized text.
 */
internal fun Strings.describe(field: PasswordField, error: EditError): String =
  if (field == PasswordField.Current && (error as? EditError.Rejected)?.reason?.code == "invalid") fieldErrorWrongPassword
  else describe(error)

private fun Strings.label(field: PasswordField) = when (field) {
  PasswordField.Current -> accountCurrentPasswordLabel
  PasswordField.New -> accountNewPasswordLabel
  PasswordField.Confirmation -> accountConfirmPasswordLabel
}
