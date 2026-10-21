package pl.llp.aircasting.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

@Composable
fun DevicePickerScreen(
  devices: List<DiscoveredAirBeam>,
  selected: DiscoveredAirBeam?,
  permissionGranted: Boolean,
  onRequestPermission: () -> Unit,
  onSelect: (DiscoveredAirBeam) -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
) {
  val strings = LocalStrings.current
  WizardScaffold(
    progress = NewSessionStep.Device.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.next, onNext, enabled = selected != null) },
  ) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
      Text(
        strings.newSessionDeviceTitle,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        strings.newSessionDeviceBody,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(24.dp))
      Text(
        strings.newSessionDeviceAirBeamHeader,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
      )
      Spacer(Modifier.height(16.dp))
      if (!permissionGranted) {
        BluetoothPermissionCard(onAllow = onRequestPermission)
      } else if (devices.isNotEmpty()) {
        Column(
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        ) {
          devices.forEach { device ->
            DeviceRow(
              name = device.name,
              selected = device.id == selected?.id,
              onSelect = { onSelect(device) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeviceRow(name: String, selected: Boolean, onSelect: () -> Unit) {
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // No maxLines/overflow: long names wrap onto more lines instead of being cut off with "…"
    Text(
      name,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    RadioButton(
      selected = selected,
      onClick = null,
      modifier = Modifier.size(48.dp),
      colors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    )
  }
}

@Composable
private fun BluetoothPermissionCard(onAllow: () -> Unit) {
  val strings = LocalStrings.current
  Column(
    Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      strings.bluetoothPermissionNeeded,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    TextButton(onClick = onAllow) { Text(strings.bluetoothPermissionAllow) }
  }
}