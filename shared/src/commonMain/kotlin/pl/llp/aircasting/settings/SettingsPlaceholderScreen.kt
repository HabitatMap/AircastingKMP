package pl.llp.aircasting.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.title

@Composable
fun SettingsPlaceholderScreen(route: SettingsRoute, onBack: () -> Unit) {
  val strings = LocalStrings.current
  SettingsScaffold(title = strings.title(route), onBack = onBack) { padding ->
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
      Text(strings.title(route), style = MaterialTheme.typography.titleLarge)
    }
  }
}
