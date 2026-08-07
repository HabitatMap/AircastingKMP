package pl.llp.aircasting.settings.server

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_cancel
import aircasting.shared.generated.resources.ic_check
import aircasting.shared.generated.resources.img_custom_server_hero
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun IntroStep() {
  val strings = LocalStrings.current
  Column(modifier = Modifier.fillMaxWidth(),) {
    Image(
      painterResource(Res.drawable.img_custom_server_hero),
      contentDescription = null,
      modifier = Modifier.fillMaxWidth().height(200.dp),
    )
    Spacer(Modifier.height(24.dp))
    Text(
      strings.customServerTitle,
      modifier = Modifier.fillMaxWidth(),
      style = MaterialTheme.typography.headlineMediumEmphasized,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
      strings.customServerIntroBody,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    listOf(strings.customServerStep1, strings.customServerStep2, strings.customServerStep3)
      .forEachIndexed { index, text -> NumberedStep(index + 1, text) }
  }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
  Row(
    Modifier.fillMaxWidth().padding(bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      Modifier
        .size(32.dp)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        "$number",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
    Text(
      text,
      modifier = Modifier.align(Alignment.CenterVertically),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
fun FormStep(form: CustomServerForm, onEdit: (CustomServerForm) -> Unit, onUseOfficial: () -> Unit) {
  val strings = LocalStrings.current
  Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
    Text(
      strings.customServerTitle,
      style = MaterialTheme.typography.headlineMediumEmphasized,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      strings.customServerFormBody,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )

    ServerField(
      value = form.host,
      onValueChange = { onEdit(form.copy(host = it)) },
      label = strings.customServerUrlLabel,
      supporting = strings.customServerUrlHint,
      isError = form.hostError,
      keyboard = KeyboardType.Uri,
    )
    ServerField(
      value = form.port,
      onValueChange = { onEdit(form.copy(port = it)) },
      label = strings.customServerPortLabel,
      supporting = strings.customServerPortHint,
      isError = form.portError,
      keyboard = KeyboardType.Number,
    )
    Text(
      strings.customServerUseOfficial,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .clickable(onClick = onUseOfficial)
        .padding(horizontal = 24.dp, vertical = 16.dp),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

@Composable
private fun ServerField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  supporting: String,
  isError: Boolean,
  keyboard: KeyboardType,
) {
  val strings = LocalStrings.current
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    supportingText = { Text(supporting) },
    isError = isError,
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(
            painterResource(Res.drawable.ic_cancel),
            contentDescription = strings.customServerClearField,
            modifier = Modifier.size(24.dp),
          )
        }
      }
    },
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
      unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
    ),
  )
}

@Composable
fun StatusStep(title: String, description: String, icon: @Composable () -> Unit) {
  Column(
    Modifier.fillMaxWidth().padding(top = 140.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    icon()
    Text(
      title,
      style = MaterialTheme.typography.headlineMediumEmphasized,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Text(
      description,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun StatusBadge(icon: DrawableResource, tint: Color, background: Color) {
  Box(
    Modifier.size(96.dp).background(background, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp), tint = tint)
  }
}

@Composable
fun SpinnerBadge() {
  Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
      modifier = Modifier.size(44.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      trackColor = MaterialTheme.colorScheme.inversePrimary,
      strokeWidth = 5.dp,
    )
  }
}