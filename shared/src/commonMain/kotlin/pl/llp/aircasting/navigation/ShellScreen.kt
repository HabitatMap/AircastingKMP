package pl.llp.aircasting.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.record.RecordingViewModel

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
    // TEMP: until the session dashboard exists, the only way to stop a session.
    topBar = { if (recording) RecordingBanner(onStop = recorder::stop) },
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