package pl.llp.aircasting.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.record.RecordingViewModel
import pl.llp.aircasting.scan.ConnectionUiState
import pl.llp.aircasting.scan.ScanUiState
import pl.llp.aircasting.scan.ScanViewModel

@Composable
fun ShellScreen(
  onRequestLocation: () -> Unit,
  onOpenSettings: () -> Unit,
  onStartNewSession: () -> Unit,
) {
  val recorder = koinViewModel<RecordingViewModel>()
  val recording by recorder.recording.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { onRequestLocation() }

  var selected by rememberSaveable { mutableStateOf(AppTab.Home) }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    // TEMP: until the connect step and the session dashboard exist.
    topBar = { if (recording) RecordingBanner(onStop = recorder::stop) else ConnectBanner() },
    bottomBar = { AircastingNavBar(selected, onSelect = { selected = it }) },
  ) { padding ->
    Box(Modifier.padding(padding)) {
      TabContent(selected, onRequestLocation, onOpenSettings, onStartNewSession)
    }
  }
}

@Composable
private fun RecordingBanner(onStop: () -> Unit) {
  Surface(color = MaterialTheme.colorScheme.errorContainer) {
    Row(
      Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Recording…", Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
      TextButton(onClick = onStop) { Text("Stop") }
    }
  }
}

// TEMP: shows the link state and connects to a scanned AirBeam.
@Composable
private fun ConnectBanner() {
  val scan = koinViewModel<ScanViewModel>()
  val connection by scan.connection.collectAsStateWithLifecycle()
  var picking by remember { mutableStateOf(false) }

  Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
    Row(
      Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val label = when (val c = connection) {
        ConnectionUiState.None -> "No AirBeam"
        ConnectionUiState.Connecting -> "Connecting…"
        is ConnectionUiState.Connected -> "Connected: ${c.device}"
        is ConnectionUiState.Failed -> "Failed: ${c.reason}"
      }
      Text(label, Modifier.weight(1f))
      if (connection !is ConnectionUiState.Connected && connection != ConnectionUiState.Connecting) {
        TextButton(onClick = { picking = true }) { Text("Connect") }
      }
    }
  }
  if (picking) DevicePicker(scan, onDismiss = { picking = false })
}

@Composable
private fun DevicePicker(scan: ScanViewModel, onDismiss: () -> Unit) {
  val state by scan.state.collectAsStateWithLifecycle()
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    title = { Text("AirBeams nearby") },
    text = {
      when (val s = state) {
        ScanUiState.Idle -> Text("Scanning…")
        is ScanUiState.Error -> Text("Can't scan: ${s.reason}")
        is ScanUiState.Scanning -> Column {
          if (s.devices.isEmpty()) Text("Scanning…")
          s.devices.forEach { device ->
            TextButton(onClick = { scan.onConnectClicked(device); onDismiss() }) {
              Text("${device.name} (${device.device})")
            }
          }
        }
      }
    },
  )
}
