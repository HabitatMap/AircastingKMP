package pl.llp.aircasting.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.settings.SettingsScaffold
import pl.llp.aircasting.ui.ageLabel
import pl.llp.aircasting.ui.components.DotSeparatedRow
import pl.llp.aircasting.ui.components.ErrorState
import pl.llp.aircasting.ui.components.LevelBadge
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.components.TintedTag
import kotlin.time.Clock

/** Figma "Single station view" (693:1565), header + stats; graph/map/slider come later. */
@Composable
fun StationDetailsScreen(
  state: StationDetailsState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSelectStream: (streamId: Long) -> Unit,
) {
  val strings = LocalStrings.current
  // App-bar actions (export, share, favourite, alert) stay hidden until they work.
  SettingsScaffold(title = "", onBack = onBack) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        StationDetailsState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        StationDetailsState.Error -> ErrorState(onRetry, Modifier.align(Alignment.Center))
        is StationDetailsState.Content -> Column(
          Modifier.verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val details = state.details
          val selected = state.selected
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TintedTag(details.source.tag(strings))
            // NOW value's level; hidden when the sensor has no local threshold.
            selected?.let { state.levelOf(it.value) }?.let { LevelBadge(it) }
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              details.title,
              style = MaterialTheme.typography.titleLargeEmphasized,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            // Design's "Location name" line needs reverse geocoding — later slice.
            val clock = koinInject<Clock>()
            // One reading per loaded state, like NearbyStations: the label doesn't tick while open.
            val now = remember(state) { clock.now() }
            DotSeparatedRow(
              listOfNotNull(
                strings.stationOutdoor, // Explore only returns outdoor sessions
                details.deviceModel?.let(::deviceDisplayName) ?: details.provider,
                selected?.measuredAt?.let { ageLabel(it, now, strings) },
              ),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            details.streams.forEach { stream ->
              StreamChip(
                streamChipLabel(stream.sensorName),
                selected = stream.id == state.selectedStreamId,
                onClick = { onSelectStream(stream.id) },
              )
            }
          }
          selected?.let { stream ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Stat(strings.stationNow, stream.value, stream.unitSymbol, state.levelOf(stream.value))
              Stat(strings.stationMin24h, stream.min24h, stream.unitSymbol, state.levelOf(stream.min24h))
              Stat(strings.stationMax24h, stream.max24h, stream.unitSymbol, state.levelOf(stream.max24h))
            }
            // Figma draws the graph edge to edge, through the screen padding.
            StationGraph(stream, state.threshold, Modifier.bleed(ScreenPadding))
          }
        }
      }
    }
  }
}

private val ScreenPadding = 24.dp

/** Widens the child by [padding] on each side, undoing the parent's horizontal padding. */
private fun Modifier.bleed(padding: Dp): Modifier = layout { measurable, constraints ->
  val extra = padding.roundToPx() * 2
  val width = constraints.maxWidth + extra
  val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
  layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

private fun DataSource.tag(strings: Strings): String = when (this) {
  DataSource.AirBeam -> strings.exploreSourceAirBeam.uppercase() // "AIRBEAM FIXED"
  DataSource.Government -> strings.govMonitor
}

/**
 * Figma stream "Tag": 28dp pill, 1dp border, brand-accent border + tint when selected.
 * Not SelectableChip — that's the 44dp, 10dp-corner option tile.
 */
@Composable
private fun StreamChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.clip(CircleShape).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    shape = CircleShape,
    color = if (selected) colors.primaryContainer.copy(alpha = 0.08f) else Color.Transparent,
    contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    border = BorderStroke(1.dp, if (selected) colors.primaryContainer else colors.outlineVariant),
  ) {
    Text(label, Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
/** [value] null (no reading) → a dash. */
private fun Stat(label: String, value: Int?, unit: String, level: MeasurementLevel?) {
  Column {
    Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      level?.let { LevelDot(it) }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          value?.toString() ?: "–",
          Modifier.alignByBaseline(),
          style = MaterialTheme.typography.headlineSmallEmphasized,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(unit, Modifier.alignByBaseline(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
      }
    }
  }
}
