package pl.llp.aircasting.explore

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_check
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsSheet

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
    Column(
      Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionLabel(strings.exploreFilterSource)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Option(strings.exploreShowAll, draft.source == null) { onEdit(draft.copy(source = null)) }
        Option(strings.exploreOnlyAirBeam, draft.source == DataSource.AirBeam) {
          onEdit(draft.copy(source = DataSource.AirBeam))
        }
        Option(strings.exploreOnlyGovernment, draft.source == DataSource.Government) {
          onEdit(draft.copy(source = DataSource.Government))
        }
      }

      SectionLabel(strings.exploreFilterParameter)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Option(strings.exploreShowAll, draft.measurementTypes.isEmpty()) {
          onEdit(draft.copy(measurementTypes = emptySet()))
        }
        MeasurementType.entries.forEach { type ->
          Option(strings.label(type), type in draft.measurementTypes) { onEdit(draft.toggled(type)) }
        }
      }

      Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TextButton(onClick = { onEdit(ExploreFilters()) }) {
          Text(strings.exploreClearAll, color = MaterialTheme.colorScheme.onSurface)
        }
        OutlinedButton(
          onClick = onConfirm,
          modifier = Modifier.weight(1f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        ) {
          Text(strings.exploreShowResults(sheet.count), color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text,
    modifier = Modifier.padding(top = 8.dp),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
    leadingIcon = if (selected) {
      { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
    } else null,
    colors = FilterChipDefaults.filterChipColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
    ),
    border = FilterChipDefaults.filterChipBorder(
      enabled = true,
      selected = selected,
      selectedBorderColor = MaterialTheme.colorScheme.primaryContainer,
      selectedBorderWidth = 1.dp,
    ),
  )
}
