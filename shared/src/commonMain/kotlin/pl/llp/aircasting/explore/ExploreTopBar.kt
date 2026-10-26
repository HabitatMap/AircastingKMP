package pl.llp.aircasting.explore

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_filter_list
import aircasting.shared.generated.resources.ic_search
import aircasting.shared.generated.resources.ic_x
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings


/**
 * White elevation-1 header (zIndex so the map below doesn't paint over the shadow).
 * Insets follow Figma: search bar 28dp from the left, filter icon 20dp from the right.
 */
@Composable
fun ExploreTopBar(
  filters: ExploreFilters,
  query: String,
  onQueryChange: (String) -> Unit,
  onOpenFilters: () -> Unit,
  onApplyFilters: (ExploreFilters) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val colors = MaterialTheme.colorScheme
  val hasFilters = filters != ExploreFilters()
  Column(
    modifier
      .zIndex(1f)
      .background(colors.surfaceContainerLowest)
      .statusBarsPadding()
      // Figma's 16dp bottom; with chips, their touch-target slack already covers 8dp of it.
      .padding(top = 8.dp, bottom = if (hasFilters) 8.dp else 16.dp),
  ) {
    Row(
      Modifier.height(64.dp).padding(start = 28.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SearchPill(query, onQueryChange, Modifier.weight(1f))
      Spacer(Modifier.width(4.dp))
      IconButton(onClick = onOpenFilters) {
        Icon(
          painterResource(Res.drawable.ic_filter_list),
          contentDescription = strings.exploreFilters,
          tint = colors.onSurface,
        )
      }
    }
    if (hasFilters) ActiveFilterChips(filters, onApplyFilters)
  }
}

/** Place search; the hint doubles as the placeholder. Name search waits on the BE (EXPLORE_SCREEN.md). */
@Composable
private fun SearchPill(query: String, onQueryChange: (String) -> Unit, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier
      .height(56.dp)
      .clip(CircleShape)
      .background(colors.surfaceContainerLowest)
      .border(BorderStroke(1.dp, colors.outline), CircleShape)
      .padding(start = 24.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier.weight(1f),
      textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
      singleLine = true,
      cursorBrush = SolidColor(colors.primary),
      decorationBox = { field ->
        if (query.isEmpty()) {
          Text(
            LocalStrings.current.exploreSearchHint,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            maxLines = 1,
          )
        }
        field()
      },
    )
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
      Icon(painterResource(Res.drawable.ic_search), contentDescription = null, tint = colors.onSurfaceVariant)
    }
  }
}

/** One chip per applied filter, parameters first then source (Figma order). ✕ applies immediately — no sheet, no draft. */
@Composable
private fun ActiveFilterChips(filters: ExploreFilters, onApplyFilters: (ExploreFilters) -> Unit) {
  val strings = LocalStrings.current
  Row(
    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 26.dp),
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

/**
 * 32dp-tall pill (16dp corners, primaryContainer @16%). Clickable Surface pads its layout to the
 * 48dp touch target, which supplies Figma's 8dp gap above and 8dp of the bottom padding.
 */
@Composable
private fun ActiveChip(label: String, onRemove: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onRemove,
    shape = RoundedCornerShape(16.dp),
    color = colors.primaryContainer.copy(alpha = 0.16f),
    contentColor = colors.onPrimaryContainer,
  ) {
    Row(
      Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
      Icon(
        painterResource(Res.drawable.ic_x),
        contentDescription = LocalStrings.current.exploreRemoveFilter(label),
        modifier = Modifier.size(16.dp),
      )
    }
  }
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
