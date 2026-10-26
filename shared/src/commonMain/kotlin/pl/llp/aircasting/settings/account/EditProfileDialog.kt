package pl.llp.aircasting.settings.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings

@Composable
internal fun EditProfileDialog(
  field: ProfileField,
  current: String,
  error: EditError?,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  var value by remember(field) { mutableStateOf(current) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (field == ProfileField.Email) strings.accountChangeEmail else strings.accountChangeUsername)
    },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (field == ProfileField.Email) strings.authEmailLabel else strings.authUsernameLabel) },
        keyboardOptions = KeyboardOptions(
          keyboardType = if (field == ProfileField.Email) KeyboardType.Email else KeyboardType.Text,
        ),
        isError = error != null,
        supportingText = error?.let { { Text(strings.describe(it), color = MaterialTheme.colorScheme.error) } },
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text(strings.accountSave) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

internal fun Strings.describe(error: EditError): String = when (error) {
  EditError.Unexpected -> accountUpdateFailed
  EditError.Refused -> accountUpdateRefused
  is EditError.Rejected -> with(error.reason) {
    when (code) {
      "taken" -> fieldErrorTaken
      "blank" -> fieldErrorBlank
      "too_short" -> params["min"]?.let(fieldErrorTooShort) ?: message
      "too_long" -> params["max"]?.let(fieldErrorTooLong) ?: message
      "confirmation" -> fieldErrorPasswordMismatch
      // `invalid` is the spec's "prose but no code yet": the English prose may be more specific,
      // but a translated "invalid value" beats showing a French user English.
      "invalid", "invalid_format" -> fieldErrorInvalid
      // Any code added after this build: the spec's contract is "fall back to message".
      else -> message
    }
  }
}
