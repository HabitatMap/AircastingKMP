package com.lunarlogic.aircasting.home.components

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.air_quality_good
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_help
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.worstLevel
import com.lunarlogic.aircasting.home.HomeUiState
import com.lunarlogic.aircasting.ui.theme.AircastingTheme
import com.lunarlogic.aircasting.ui.theme.LocalAqColors
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant

/** The nearest-station air-quality card (Figma "Air quality card", node 277:7291). */
@Composable
internal fun AirQualityCard(aq: HomeUiState.AirQuality, onRequestLocation: () -> Unit) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      when (aq) {
        is HomeUiState.AirQuality.Loaded -> {
          // Empty readings can't reach Loaded, but the default keeps this `when` total.
          val level = aq.readings.worstLevel() ?: MeasurementLevel.LOW
          AirQualityHeader(status = level.aqStatus(), level = level)
          PollutantRow(aq.readings)
          StationSelectorRow(aq.stationName, aq.distanceMeters)
        }

        HomeUiState.AirQuality.NoReadings -> {
          Text("No air quality data available", style = MaterialTheme.typography.titleMedium)
          Text(
            "We couldn't find current readings for this location. Check again later.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        HomeUiState.AirQuality.NoLocation -> {
          Text("No nearby station found", style = MaterialTheme.typography.titleMedium)
          Text(
            "We couldn't match your location to a reporting station. Try enabling precise location.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedButton(onClick = onRequestLocation) { Text("Turn on location services") }
        }
      }
    }
  }
}

@Composable
private fun AirQualityHeader(status: AqStatus, level: MeasurementLevel) {
  val aqColor = LocalAqColors.current.forLevel(level)
  Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Image(
      painter = painterResource(Res.drawable.air_quality_good), // TODO: per-level art; only "Good" exported
      contentDescription = null,
      modifier = Modifier.size(96.dp),
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("AIR QUALITY", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
      Text(
        status.label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = aqColor,
      )
      Text(status.description, style = MaterialTheme.typography.bodySmall, color = aqColor)
    }
  }
}

@Composable
private fun PollutantRow(readings: List<PollutantReading>) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    readings.forEach { PollutantCell(it, Modifier.weight(1f)) }
  }
}

@Composable
private fun PollutantCell(reading: PollutantReading, modifier: Modifier = Modifier) {
  Column(
    modifier
      .border(1.dp, LocalAqColors.current.forLevel(reading.level), RoundedCornerShape(8.dp)) // per-pollutant color
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        reading.pollutant.label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Icon(
        painterResource(Res.drawable.ic_help),
        contentDescription = null,
        Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.outline,
      )
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        reading.value.toString(),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(reading.unitSymbol, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
private fun StationSelectorRow(name: String, distanceMeters: Double) {
  Row(
    Modifier.fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
      Text(distanceLabel(distanceMeters), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
    Icon(
      painterResource(Res.drawable.ic_arrow_forward_ios),
      contentDescription = null,
      Modifier.size(20.dp),
      tint = MaterialTheme.colorScheme.outline,
    )
  }
}

@Preview
@Composable
private fun AirQualityCardLoadedPreview() {
  AircastingTheme {
    AirQualityCard(
      aq = HomeUiState.AirQuality.Loaded(
        stationName = "Central Park Station, New York",
        distanceMeters = 643.7, // ≈ 0.4 mile
        readings = listOf(
          PollutantReading(Pollutant.PM25, 4.2, "µg/m³", MeasurementLevel.LOW),
          PollutantReading(Pollutant.NO2, 9.1, "ppb", MeasurementLevel.LOW),
          PollutantReading(Pollutant.OZONE, 4.2, "ppb", MeasurementLevel.LOW),
        ),
        updatedAt = Instant.fromEpochSeconds(0),
      ),
      onRequestLocation = {},
    )
  }
}

@Preview
@Composable
private fun AirQualityCardNoLocationPreview() {
  AircastingTheme { AirQualityCard(HomeUiState.AirQuality.NoLocation, onRequestLocation = {}) }
}
