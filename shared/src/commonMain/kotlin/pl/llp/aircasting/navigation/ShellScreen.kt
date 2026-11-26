package pl.llp.aircasting.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun ShellScreen(
  selected: AppTab,
  onSelect: (AppTab) -> Unit,
  onRequestLocation: () -> Unit,
  onOpenSettings: () -> Unit,
  onStartNewSession: () -> Unit,
  onOpenStation: (uuid: String) -> Unit,
  onOpenSession: (uuid: String, sensorName: String?) -> Unit,
  onOpenFixedSession: (uuid: String, sensorName: String?) -> Unit,
  onOpenPastSessions: () -> Unit,
) {
  LaunchedEffect(Unit) { onRequestLocation() }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = { AircastingNavBar(selected, onSelect = onSelect) },
  ) { padding ->
    // Consume what we pad, so tabs using statusBarsPadding() etc. don't apply the inset twice.
    Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
      TabContent(
        selected, onRequestLocation, onOpenSettings, onStartNewSession, onOpenStation, onOpenSession,
        onOpenPastSessions, onOpenFixedSession,
      )
    }
  }
}
