package com.lunarlogic.aircasting.home.components

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_more_vert
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.StationWithDistance
import com.lunarlogic.aircasting.domain.worstLevel
import com.lunarlogic.aircasting.i18n.LocalStrings
import com.lunarlogic.aircasting.ui.theme.AircastingTheme
import com.lunarlogic.aircasting.ui.theme.LocalAqColors
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.time.Clock

/** Horizontal carousel of nearby government-monitor stations (Figma node 293:17519). */
@Composable
internal fun NearbyStationsSection(stations: List<StationWithDistance>, onViewMap: () -> Unit = {}) {
  val clock = koinInject<Clock>()
  val now = remember(stations) { clock.now() } // one clock reading per loaded list
  val strings = LocalStrings.current
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(strings.nearbyStationsTitle, style = MaterialTheme.typography.titleLarge)
      TextButton(onClick = onViewMap) {
        Text(strings.viewMap)
        Icon(painterResource(Res.drawable.ic_arrow_forward_ios), contentDescription = null, Modifier.size(20.dp))
      }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(stations) { item ->
        StationCard(
          name = item.station.name,
          subtitle = distanceLabel(item.distanceMeters, strings), // we have distance, not an address
          updatedLabel = ageLabel(item.station.updatedAt, now, strings),
          readings = item.station.readings,
        )
      }
    }
  }
}

@Composable
private fun StationCard(
  name: String,
  subtitle: String,
  updatedLabel: String,
  readings: List<PollutantReading>,
) {
  val level = readings.worstLevel() ?: MeasurementLevel.LOW
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier = Modifier.width(332.dp),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GovMonitorTag()
            StatusBadge(level)
          }
          Icon(
            painterResource(Res.drawable.ic_more_vert),
            contentDescription = null,
            Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.outline,
          ) // TODO: options menu
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(updatedLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        readings.forEach { StationPollutantCell(it, Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun GovMonitorTag() {
  Box(
    Modifier.clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  ) {
    Text(LocalStrings.current.govMonitor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
  }
}

@Composable
private fun StatusBadge(level: MeasurementLevel) {
  val c = LocalAqColors.current.forLevel(level)
  Box(
    Modifier.clip(RoundedCornerShape(100.dp))
      .background(c.copy(alpha = 0.12f))
      .padding(horizontal = 8.dp, vertical = 2.dp),
  ) {
    Text(level.aqStatus(LocalStrings.current).label, style = MaterialTheme.typography.labelSmall, color = c)
  }
}

@Composable
private fun StationPollutantCell(reading: PollutantReading, modifier: Modifier = Modifier) {
  Column(
    modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(LocalAqColors.current.forLevel(reading.level)))
        Text(reading.value.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
      }
      Text(reading.unitSymbol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
    Text(reading.pollutant.label(LocalStrings.current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
  }
}

@Preview
@Composable
private fun StationCardPreview() {
  AircastingTheme {
    StationCard(
      name = "Downtown Civic Station",
      subtitle = "0.4 mile away",
      updatedLabel = "2 min ago",
      readings = listOf(
        PollutantReading(Pollutant.PM25, 8.2, "µg/m³", MeasurementLevel.LOW),
        PollutantReading(Pollutant.PM25, 15.0, "ppb", MeasurementLevel.LOW),
        PollutantReading(Pollutant.NO2, 18.3, "ppb", MeasurementLevel.LOW),
      ),
    )
  }
}
