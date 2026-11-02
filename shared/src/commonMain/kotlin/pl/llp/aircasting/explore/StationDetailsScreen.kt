package pl.llp.aircasting.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.ui.ageLabel
import pl.llp.aircasting.ui.aqStatus
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsScaffold
import pl.llp.aircasting.ui.theme.LocalAqColors
import kotlin.time.Clock

@Composable
fun StationDetailsScreen(
  state: StationDetailsState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSelectStream: (streamId: Long) -> Unit,
) {
  val strings = LocalStrings.current
  // Title lives in the body (design), so the bar only carries back for now.
  SettingsScaffold(title = "", onBack = onBack) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        StationDetailsState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        StationDetailsState.Error -> Column(
          Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(strings.errorTitle, style = MaterialTheme.typography.bodyLarge)
          TextButton(onClick = onRetry) { Text(strings.errorRetry) }
        }
        is StationDetailsState.Content -> Column(
          Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val details = state.details
          val nowLevel = state.selected?.let { state.levelOf(it.value) }
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              when (details.source) {
                DataSource.AirBeam -> "AIRBEAM FIXED"
                DataSource.Government -> "GOV"
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            // Design's "Moderate" badge: the NOW value's level, hidden when there's no threshold.
            nowLevel?.let { level ->
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelDot(level)
                Text(level.aqStatus(strings).label, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
          Text(details.title, style = MaterialTheme.typography.headlineSmall)
          val clock = koinInject<Clock>()
          // One reading per loaded state, like NearbyStations: the label doesn't tick while open.
          val now = remember(state) { clock.now() }
          Text(
            listOfNotNull(
              details.deviceModel ?: details.provider,
              state.selected?.let { ageLabel(it.measuredAt, now, strings) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            details.streams.forEach { stream ->
              FilterChip(
                selected = stream.id == state.selectedStreamId,
                onClick = { onSelectStream(stream.id) },
                // "AirBeam-PM2.5" → "PM2.5"
                label = { Text(stream.sensorName.substringAfter('-')) },
              )
            }
          }
          state.selected?.let { stream ->
            Row(Modifier.fillMaxWidth()) {
              Stat(strings.stationNow, stream.value, stream.unitSymbol, state.levelOf(stream.value), Modifier.weight(1f))
              Stat(strings.stationMin24h, stream.min24h, stream.unitSymbol, state.levelOf(stream.min24h), Modifier.weight(1f))
              Stat(strings.stationMax24h, stream.max24h, stream.unitSymbol, state.levelOf(stream.max24h), Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Stat(label: String, value: Int, unit: String, level: MeasurementLevel?, modifier: Modifier) {
  Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      level?.let { LevelDot(it) }
      Text("$value", style = MaterialTheme.typography.headlineSmall)
    }
    Text(unit, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun LevelDot(level: MeasurementLevel) {
  Box(Modifier.size(10.dp).background(LocalAqColors.current.forLevel(level), CircleShape))
}