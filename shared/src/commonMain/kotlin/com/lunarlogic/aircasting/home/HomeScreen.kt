package com.lunarlogic.aircasting.home

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.air_quality_good
import aircasting.shared.generated.resources.ic_help
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.StationWithDistance
import com.lunarlogic.aircasting.domain.ageLabelFrom
import com.lunarlogic.aircasting.domain.worstLevel
import com.lunarlogic.aircasting.home.HomeUiState.AirQuality.Loaded
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant

private val SurfaceCell = Color(0xFFFCF8F8)   // schemes/surface
private val OnSurfaceVariantAlt = Color(0xFF49454F)
private val GovTagBg = Color(0x1400B2EF)      // primary-container @ 8%
private val GovTagText = Color(0xFF004059)
private val StationName = Color(0xFF0D0D12)
private val CardWhite = Color(0xFFFFFFFF)
private val Outline = Color(0xFF75777B)
private val OutlineVariant = Color(0xFFCAC4D0)
private val OnSurfaceVariant = Color(0xFF44474A)
private val OnBackground = Color(0xFF171C20)

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
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      when (aq) {
        is HomeUiState.AirQuality.Loaded -> {
          // Empty readings can't reach Loaded, but default keeps this total.
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
      Text("AIR QUALITY", style = MaterialTheme.typography.titleSmall, color = Outline)
      Text(
        status.label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = levelColor(level),
      )
      Text(
        status.description,
        style = MaterialTheme.typography.bodySmall,
        color = levelColor(level)
      )
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
      .border(1.dp, levelColor(reading.level), RoundedCornerShape(8.dp)) // per-pollutant color
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        reading.pollutant.label,
        style = MaterialTheme.typography.labelLarge,
        color = OnSurfaceVariant
      )
      Icon(
        painterResource(Res.drawable.ic_help),
        contentDescription = null,
        Modifier.size(16.dp),
        tint = Outline
      )
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        reading.value.toString(),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = OnBackground,
      )
      Text(reading.unitSymbol, style = MaterialTheme.typography.labelMedium, color = Outline)
    }
  }
}

@Composable
private fun StationSelectorRow(name: String, distanceMeters: Double) {
  Row(
    Modifier.fillMaxWidth()
      .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(name, style = MaterialTheme.typography.titleSmall, color = OnBackground)
      Text(
        distanceLabel(distanceMeters),
        style = MaterialTheme.typography.bodySmall,
        color = Outline
      )
    }
    Icon(
      Icons.Filled.KeyboardArrowRight,
      contentDescription = null,
      Modifier.size(20.dp),
      tint = Outline
    )
  }
}

private data class AqStatus(val label: String, val description: String)

private fun MeasurementLevel.aqStatus(): AqStatus = when (this) {
  MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW ->
    AqStatus("Good", "The air outside is clean. A great time to enjoy activities outside.")
  // TODO: copy + illustrations for these come from the other Figma states — placeholder for now.
  MeasurementLevel.MEDIUM -> AqStatus("Moderate", "Air quality is acceptable for most people.")
  MeasurementLevel.HIGH -> AqStatus(
    "Unhealthy for sensitive groups",
    "Sensitive groups should limit outdoor exertion."
  )

  MeasurementLevel.VERY_HIGH -> AqStatus(
    "Unhealthy",
    "Everyone may begin to feel effects. Limit outdoor time."
  )

  MeasurementLevel.EXTREMELY_HIGH -> AqStatus(
    "Hazardous",
    "Health warning. Avoid outdoor activity."
  )
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
private fun NearbyStationsSection(stations: List<StationWithDistance>, onViewMap: () -> Unit = {}) {
  val clock = koinInject<Clock>()
  val now = remember(stations) { clock.now() }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Nearby stations", style = MaterialTheme.typography.titleLarge)
      TextButton(onClick = onViewMap) {
        Text("View map")
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(20.dp))
      }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(stations) { item ->
        StationCard(
          name = item.station.name,
          subtitle = distanceLabel(item.distanceMeters),        // we have distance, not an address
          // TODO: Actual updatedAt field
          updatedLabel = item.station.updatedAt.ageLabelFrom(now),
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
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    modifier = Modifier.width(332.dp),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            GovMonitorTag()
            StatusBadge(level)
          }
          Icon(
            Icons.Filled.MoreVert,
            contentDescription = null,
            Modifier.size(24.dp),
            tint = Outline
          ) // TODO: options menu
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = StationName,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantAlt)
            Text("•", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantAlt)
            Text(
              updatedLabel,
              style = MaterialTheme.typography.bodySmall,
              color = OnSurfaceVariantAlt
            )
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
    Modifier.clip(RoundedCornerShape(8.dp)).background(GovTagBg).padding(horizontal = 6.dp, vertical = 2.dp),
  ) {
    Text("GOV MONITOR", style = MaterialTheme.typography.labelSmall, color = GovTagText)
  }
}
@Composable
private fun StatusBadge(level: MeasurementLevel) {
  val c = levelColor(level)
  Box(
    Modifier.clip(RoundedCornerShape(100.dp)).background(c.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
  ) {
    Text(level.aqStatus().label, style = MaterialTheme.typography.labelSmall, color = c)
  }
}

@Composable
private fun StationPollutantCell(reading: PollutantReading, modifier: Modifier = Modifier) {
  Column(
    modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCell).padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(levelColor(reading.level)))
        Text(reading.value.toString(), style = MaterialTheme.typography.titleMedium, color = OnBackground)
      }
      Text(reading.unitSymbol, style = MaterialTheme.typography.labelSmall, color = Outline)
    }
    Text(reading.pollutant.label, style = MaterialTheme.typography.bodySmall, color = Outline)
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

private val previewReadings = listOf(
  PollutantReading(Pollutant.PM25, 4.2, "µg/m³", MeasurementLevel.LOW),
  PollutantReading(Pollutant.NO2, 9.1, "ppb", MeasurementLevel.LOW),
  PollutantReading(Pollutant.OZONE, 4.2, "ppb", MeasurementLevel.LOW),
)

@Preview
@Composable
private fun AirQualityCardLoadedPreview() {
  MaterialTheme {
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
  MaterialTheme { AirQualityCard(HomeUiState.AirQuality.NoLocation, onRequestLocation = {}) }
}

@Preview
@Composable
private fun StationCardPreview() {
  MaterialTheme {
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