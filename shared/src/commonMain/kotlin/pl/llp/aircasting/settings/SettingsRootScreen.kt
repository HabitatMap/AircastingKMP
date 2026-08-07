package pl.llp.aircasting.settings

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_airbeam
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_info
import aircasting.shared.generated.resources.ic_person
import aircasting.shared.generated.resources.ic_settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
      Column(
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerLowest)
          .padding(horizontal = 16.dp),
      ) {
        SettingsRoute.sections.forEach { section ->
          SectionRow(section, onClick = { onSection(section) })
        }
      }
      Spacer(Modifier.weight(1f))
      SettingsFooter(Modifier.fillMaxWidth().padding(bottom = 40.dp))
    }
  }
}

@Composable
private fun SectionRow(route: SettingsRoute, onClick: () -> Unit) {
  val strings = LocalStrings.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    route.icon()?.let { icon ->
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
    Column(Modifier.weight(1f)) {
      Text(
        strings.title(route),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      strings.subtitle(route)?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Icon(
      painterResource(Res.drawable.ic_arrow_forward_ios),
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
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