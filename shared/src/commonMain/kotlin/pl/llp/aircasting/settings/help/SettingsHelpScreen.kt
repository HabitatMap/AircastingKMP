package pl.llp.aircasting.settings.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.title
import pl.llp.aircasting.settings.SettingsLinkRow
import pl.llp.aircasting.settings.SettingsScaffold
import pl.llp.aircasting.settings.SettingsSection

@Composable
fun SettingsHelpScreen(
  state: HelpScreenState,
  onBack: () -> Unit,
  onOpen: (url: String) -> Unit,
  onRetry: () -> Unit,
) {
  val strings = LocalStrings.current
  SettingsScaffold(title = strings.title(SettingsRoute.Help), onBack = onBack) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        HelpScreenState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        HelpScreenState.Error -> Column(
          Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(strings.errorTitle, style = MaterialTheme.typography.bodyLarge)
          TextButton(onClick = onRetry) { Text(strings.errorRetry) }
        }
        // Same scroll/padding/spacing as SettingsAppScreen.
        is HelpScreenState.Content -> Column(
          Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
          verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
          LinkSection(strings.settingsHelpTitle, state.links.help, onOpen)
          LinkSection(strings.helpSectionActions, state.links.actions, onOpen)
        }
      }
    }
  }
}

@Composable
private fun LinkSection(header: String, links: List<HelpLink>, onOpen: (String) -> Unit) {
  // Server-driven: an emptied list shouldn't leave an orphan header.
  if (links.isEmpty()) return
  SettingsSection(header) {
    links.forEach { link -> SettingsLinkRow(link.title) { onOpen(link.url) } }
  }
}
