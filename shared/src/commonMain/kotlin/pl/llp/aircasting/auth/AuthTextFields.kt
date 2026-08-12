package pl.llp.aircasting.auth

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_cancel
import aircasting.shared.generated.resources.ic_visibility
import aircasting.shared.generated.resources.ic_visibility_off
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

private val FieldShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

@Composable
private fun authFieldColors(): TextFieldColors = TextFieldDefaults.colors(
  focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
  unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun supportingText(error: String?): (@Composable () -> Unit)? =
  if (error == null) null else { { Text(error) } }

@Composable
fun AuthTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  error: String? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
) {
  val strings = LocalStrings.current
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = true,
    isError = error != null,
    supportingText = supportingText(error),
    shape = FieldShape,
    colors = authFieldColors(),
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(
            painterResource(Res.drawable.ic_cancel),
            contentDescription = strings.authClearField,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
  )
}

@Composable
fun AuthPasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  revealed: Boolean,
  onToggleReveal: () -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
  error: String? = null,
) {
  val strings = LocalStrings.current
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = true,
    isError = error != null,
    supportingText = supportingText(error),
    shape = FieldShape,
    colors = authFieldColors(),
    visualTransformation =
      if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = ImeAction.Done,
    ),
    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    trailingIcon = {
      IconButton(onClick = onToggleReveal) {
        Icon(
          painterResource(
            if (revealed) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
          ),
          contentDescription =
            if (revealed) strings.authHidePassword else strings.authShowPassword,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
}