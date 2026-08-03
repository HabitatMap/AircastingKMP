package pl.llp.aircasting.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ShellScreen(onRequestLocation: () -> Unit, onOpenSettings: () -> Unit) {
  var selected by rememberSaveable { mutableStateOf(AppTab.Home) }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = { AircastingNavBar(selected, onSelect = { selected = it }) },
  ) { padding ->
    Box(Modifier.padding(padding)) {
      TabContent(selected, onRequestLocation, onOpenSettings)
    }
  }
}
