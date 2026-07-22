package com.lunarlogic.aircasting.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.StationWithDistance
import kotlin.math.round

@Composable
fun HomeScreen(
  state: HomeScreenState,
  onRetry: () -> Unit,
  onRequestLocation: () -> Unit = {},
) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      HomeScreenState.Loading -> CircularProgressIndicator()
      HomeScreenState.Error -> ErrorState(onRetry)
      is HomeScreenState.Content -> HomeContent(state.ui, onRequestLocation)
    }
  }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Something went wrong.", style = MaterialTheme.typography.titleMedium)
    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
  }
}

@Composable
private fun HomeContent(ui: HomeUiState, onRequestLocation: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    AirQualityCard(ui.airQuality, onRequestLocation)
    if (ui.nearby.isNotEmpty()) NearbyStationsSection(ui.nearby)
  }
}

@Composable
private fun AirQualityCard(aq: HomeUiState.AirQuality, onRequestLocation: () -> Unit) {
  Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      when (aq) {
        HomeUiState.AirQuality.NoLocation -> {
          Text("No nearby station found", style = MaterialTheme.typography.titleMedium)
          Text(
            "We couldn't match your location to a reporting station. Try enabling precise location.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedButton(onClick = onRequestLocation) { Text("Turn on location services") }
        }

        HomeUiState.AirQuality.NoReadings -> {
          Text("No air quality data available", style = MaterialTheme.typography.titleMedium)
          Text(
            "We couldn't find current readings for this location. Check again later.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        is HomeUiState.AirQuality.Loaded -> {
          Text(aq.stationName, style = MaterialTheme.typography.titleMedium)
          Text(distanceLabel(aq.distanceMeters), style = MaterialTheme.typography.bodySmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            aq.readings.forEach { MetricChip(it) }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricChip(reading: PollutantReading) {
  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(
      Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        Modifier.size(8.dp)
          .clip(CircleShape)
          .let { it }, contentAlignment = Alignment.Center
      ) {
        Surface(Modifier.size(8.dp), shape = CircleShape, color = levelColor(reading.level)) {}
      }
      Text(
        "${reading.pollutant.label} ${reading.value} ${reading.unitSymbol}",
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

@Composable
private fun NearbyStationsSection(stations: List<StationWithDistance>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Nearby stations", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(stations) { StationCard(it) }
    }
  }
}

@Composable
private fun StationCard(item: StationWithDistance) {
  Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(220.dp)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(item.station.name, style = MaterialTheme.typography.titleSmall)
      Text(distanceLabel(item.distanceMeters), style = MaterialTheme.typography.bodySmall)
      item.station.readings.firstOrNull()
        ?.let { MetricChip(it) }
    }
  }
}

private val Pollutant.label: String
  get() = when (this) {
    Pollutant.PM25 -> "PM 2.5"
    Pollutant.NO2 -> "NO₂"
    Pollutant.OZONE -> "Ozone"
  }

private fun distanceLabel(meters: Double): String {
  val miles = round(meters / 1609.344 * 10) / 10.0
  return "$miles mile away"
}

private fun levelColor(level: MeasurementLevel): Color = when (level) {
  MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW -> Color(0xFF006E02)
  MeasurementLevel.MEDIUM -> Color(0xFFC9A400)
  MeasurementLevel.HIGH -> Color(0xFFE8720C)
  MeasurementLevel.VERY_HIGH -> Color(0xFFD32F2F)
  MeasurementLevel.EXTREMELY_HIGH -> Color(0xFF7B1FA2)
}