package pl.llp.aircasting.settings

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_airbeam
import aircasting.shared.generated.resources.ic_info
import aircasting.shared.generated.resources.ic_person
import aircasting.shared.generated.resources.ic_settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import pl.llp.aircasting.AppVersion
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.subtitle
import pl.llp.aircasting.navigation.title

@Composable
fun SettingsRootScreen(onBack: () -> Unit, onSection: (SettingsRoute) -> Unit) {
  val strings = LocalStrings.current
  SettingsScaffold(title = strings.title(SettingsRoute.Root), onBack = onBack) { padding ->
    Column(Modifier.padding(padding).padding(horizontal = 24.dp)) {
      SettingsSection(header = null) {
        SettingsRoute.sections.forEach { section ->
          SettingsLinkRow(
            title = strings.title(section),
            subtitle = strings.subtitle(section),
            subtitleMaxLines = Int.MAX_VALUE,
            leading = section.icon()?.let { icon -> { SectionIcon(icon) } },
            onClick = { onSection(section) },
          )
        }
      }
      Spacer(Modifier.weight(1f))
      SettingsFooter(Modifier.fillMaxWidth().padding(bottom = 40.dp))
    }
  }
}

@Composable
private fun SectionIcon(icon: DrawableResource) {
  Box(
    modifier = Modifier
      .size(36.dp)
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

internal fun SettingsRoute.icon(): DrawableResource? = when (this) {
  SettingsRoute.Root -> null
  SettingsRoute.Account -> Res.drawable.ic_person
  SettingsRoute.AirBeams -> Res.drawable.ic_airbeam
  SettingsRoute.AppSettings -> Res.drawable.ic_settings
  SettingsRoute.Help -> Res.drawable.ic_info
  SettingsRoute.CustomDataServer -> null
}

@Composable
private fun SettingsFooter(modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  val version = koinInject<AppVersion>()
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      strings.settingsVersion(version.name),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      strings.settingsTagline,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
