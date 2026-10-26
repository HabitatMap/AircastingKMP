package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.img_bluetooth_off
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

/** The device step when it can't scan yet: Bluetooth off, or permission missing. */
@Composable
fun DeviceStatusScreen(
  title: String,
  body: String,
  buttonLabel: String,
  onButton: () -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  showChevron: Boolean = true,
) {
  WizardScaffold(
    progress = NewSessionStep.Device.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(buttonLabel, onButton, showChevron = showChevron) },
  ) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 96.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painterResource(Res.drawable.img_bluetooth_off),
        contentDescription = null,
        modifier = Modifier.size(200.dp),
      )
      Spacer(Modifier.height(24.dp))
      Text(
        title,
        style = MaterialTheme.typography.headlineLargeEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}