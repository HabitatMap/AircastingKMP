package pl.llp.aircasting.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsSheet
import pl.llp.aircasting.ui.components.OutlinedActionButton
import pl.llp.aircasting.ui.components.SectionHeader
import pl.llp.aircasting.ui.components.SelectableChip

/** Stateless: every tap goes to the VM as an edited draft; nothing reaches the map until [onConfirm]. */
@Composable
fun ExploreFilterSheet(
  sheet: FilterSheet,
  onEdit: (ExploreFilters) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalStrings.current
  val draft = sheet.draft
  SettingsSheet(strings.exploreFilters, onDismiss = onDismiss, onConfirm = onConfirm) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
      FilterSection(strings.exploreFilterSource) {
        Option(strings.exploreShowAll, draft.source == null) { onEdit(draft.copy(source = null)) }
        Option(strings.exploreOnlyAirBeam, draft.source == DataSource.AirBeam) {
          onEdit(draft.copy(source = DataSource.AirBeam))
        }
        Option(strings.exploreOnlyGovernment, draft.source == DataSource.Government) {
          onEdit(draft.copy(source = DataSource.Government))
        }
      }
      FilterSection(strings.exploreFilterParameter) {
        Option(strings.exploreShowAll, draft.measurementTypes.isEmpty(), Role.Checkbox) {
          onEdit(draft.copy(measurementTypes = emptySet()))
        }
        MeasurementType.entries.forEach { type ->
          Option(strings.label(type), type in draft.measurementTypes, Role.Checkbox) { onEdit(draft.toggled(type)) }
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
    Row(
      Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      TextButton(
        onClick = { onEdit(ExploreFilters()) },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
      ) {
        Text(strings.exploreClearAll, style = MaterialTheme.typography.titleMedium)
      }
      OutlinedActionButton(strings.exploreShowResults(sheet.count), onConfirm, Modifier.weight(1f))
    }
  }
}

@Composable
private fun FilterSection(title: String, options: @Composable FlowRowScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SectionHeader(title)
    FlowRow(
      Modifier.padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      content = options,
    )
  }
}

@Composable
private fun Option(label: String, selected: Boolean, role: Role = Role.RadioButton, onClick: () -> Unit) {
  SelectableChip(label, selected, onClick, role = role, showCheckWhenSelected = true)
}
