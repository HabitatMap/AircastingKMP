package pl.llp.aircasting.settings.app

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.title
import pl.llp.aircasting.settings.SettingsScaffold

@Composable
fun SettingsAppScreen(
  rows: List<AppSettingRow>,
  onBack: () -> Unit,
  onToggle: (AppSetting, Boolean) -> Unit,
  onOpen: (AppSetting) -> Unit,
) {
  val strings = LocalStrings.current
  SettingsScaffold(title = strings.title(SettingsRoute.AppSettings), onBack = onBack) { padding ->
    Column(
      modifier = Modifier
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      rows.groupBy { it.setting.section }
        .forEach { (section, sectionRows) ->
          SettingsCard(strings.header(section), sectionRows, onToggle, onOpen)
        }
    }
  }
}

@Composable
private fun SettingsCard(
  header: String,
  rows: List<AppSettingRow>,
  onToggle: (AppSetting, Boolean) -> Unit,
  onOpen: (AppSetting) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Text(
      header,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.outline,
    )
    Column(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .padding(horizontal = 16.dp),
    ) {
      rows.forEach { row ->
        when (row) {
          is AppSettingRow.Toggle -> ToggleRow(row) { onToggle(row.setting, it) }
          is AppSettingRow.Link -> LinkRow(row) { onOpen(row.setting) }
        }
      }
    }
  }
}

@Composable
private fun ToggleRow(row: AppSettingRow.Toggle, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(value = row.checked, role = Role.Switch, onValueChange = onCheckedChange)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RowLabel(row.setting, Modifier.weight(1f))
    Switch(checked = row.checked, onCheckedChange = null, colors = aircastingSwitchColors())
  }
}

@Composable
private fun LinkRow(row: AppSettingRow.Link, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RowLabel(row.setting, Modifier.weight(1f))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      row.value?.let {
        Text(
          it,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        painterResource(Res.drawable.ic_arrow_forward_ios),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun RowLabel(setting: AppSetting, modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  Column(modifier) {
    Text(
      strings.label(setting),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    strings.subtitle(setting)?.let {
      Text(
        it,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun aircastingSwitchColors() = SwitchDefaults.colors(
  checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
  checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
  checkedBorderColor = Color.Transparent,
  uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
  uncheckedThumbColor = MaterialTheme.colorScheme.outlineVariant,
  uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)