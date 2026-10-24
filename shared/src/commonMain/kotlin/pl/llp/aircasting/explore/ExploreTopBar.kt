package pl.llp.aircasting.explore

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_close
import aircasting.shared.generated.resources.ic_filter_list
import aircasting.shared.generated.resources.ic_search
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings


@Composable
fun ExploreTopBar(
  filters: ExploreFilters,
  onOpenFilters: () -> Unit,
  onApplyFilters: (ExploreFilters) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  Column(
    modifier
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      SearchPill(Modifier.weight(1f))
      IconButton(onClick = onOpenFilters) {
        Icon(painterResource(Res.drawable.ic_filter_list), contentDescription = strings.exploreFilters)
      }
    }
    ActiveFilterChips(filters, onApplyFilters)
  }
}

/** Visual only until location search lands (needs platform geocoding). */
@Composable
private fun SearchPill(modifier: Modifier) {
  Row(
    modifier
      .heightIn(min = 48.dp)
      .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape)
      .padding(start = 20.dp, end = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      LocalStrings.current.exploreSearchHint,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
    Icon(painterResource(Res.drawable.ic_search), contentDescription = null)
  }
}

/** One chip per applied filter, parameters first then source (Figma order). ✕ applies immediately — no sheet, no draft. */
@Composable
private fun ActiveFilterChips(filters: ExploreFilters, onApplyFilters: (ExploreFilters) -> Unit) {
  if (filters == ExploreFilters()) return
  val strings = LocalStrings.current
  Row(
    Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    MeasurementType.entries.filter { it in filters.measurementTypes }.forEach { type ->
      ActiveChip(strings.label(type)) { onApplyFilters(filters.toggled(type)) }
    }
    filters.source?.let { source ->
      ActiveChip(strings.label(source)) { onApplyFilters(filters.copy(source = null)) }
    }
  }
}

@Composable
private fun ActiveChip(label: String, onRemove: () -> Unit) {
  InputChip(
    selected = true,
    onClick = onRemove,
    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    trailingIcon = {
      Icon(
        painterResource(Res.drawable.ic_close),
        contentDescription = LocalStrings.current.exploreRemoveFilter(label),
        modifier = Modifier.size(InputChipDefaults.IconSize),
      )
    },
    colors = InputChipDefaults.inputChipColors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
    ),
    border = null,
  )
}

internal fun Strings.label(type: MeasurementType): String = when (type) {
  MeasurementType.ParticulateMatter -> exploreParticulateMatter
  MeasurementType.NitrogenDioxide -> exploreNitrogenDioxide
  MeasurementType.Ozone -> exploreOzone
  MeasurementType.Temperature -> exploreTemperature
  MeasurementType.Humidity -> exploreHumidity
}

private fun Strings.label(source: DataSource): String = when (source) {
  DataSource.AirBeam -> exploreSourceAirBeam
  DataSource.Government -> exploreSourceGovernment
}
