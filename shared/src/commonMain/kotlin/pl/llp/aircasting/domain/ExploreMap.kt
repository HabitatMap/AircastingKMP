package pl.llp.aircasting.domain

import io.ktor.openapi.ReferenceOr.Companion.value
import kotlin.math.log2
import kotlin.time.Instant

/**
 * The `measurement_type` catalogue the Explore map filters by. [apiName] is the wire value,
 * [unitSymbol] the unit the server's values are in (temperature: °F, the server default).
 */
enum class MeasurementType(val apiName: String, val unitSymbol: String) {
  ParticulateMatter("Particulate Matter", "µg/m³"),
  NitrogenDioxide("Nitrogen Dioxide", "ppb"),
  Ozone("Ozone", "ppb"),
  Temperature("Temperature", "°F"),
  Humidity("Humidity", "%"),
}

enum class DataSource(val apiName: String) {
  AirBeam("airbeam"),
  Government("government"),
}

/** What the map is showing: visible bounds + the map SDK's fractional zoom. */
data class MapViewport(val bounds: GeoSquare, val zoom: Double)

/** A marker on the Explore map. */
sealed interface MapItem {
  val location: GeoLocation
}

/** Several items sharing a server grid cell. Never colour-coded; zooming in splits it. */
data class MapCluster(override val location: GeoLocation, val count: Int) : MapItem

/** A station's reading for one selected [type]. [id] keys the stream-detail endpoints, unique only within the station's source. */
data class MapStream(val id: Long, val type: MeasurementType, val value: Int, val measuredAt: Instant)
/** One AirBeam fixed session or government station, with a stream per selected type it reports. */
data class MapStation(
  val uuid: String,
  val source: DataSource,
  val title: String,
  val provider: String,
  override val location: GeoLocation,
  val streams: List<MapStream>,
) : MapItem {
  /** This station's reading for [type]; null when it doesn't report it. */
  fun valueFor(type: MeasurementType): Int? = streams.find { it.type == type }?.value
}


/**
 * The one sensor whose thresholds colour this source's [type] value; null if the source
 * doesn't measure it. Mirrors the BE `Explore::Parameters::CATALOGUE`
 * (docs/API_V3.md → Explore map).
 */
fun DataSource.sensorNameFor(type: MeasurementType): String? = when (this) {
  DataSource.AirBeam -> when (type) {
    MeasurementType.ParticulateMatter -> "AirBeam-PM2.5"
    MeasurementType.Temperature -> "AirBeam-F"
    MeasurementType.Humidity -> "AirBeam-RH"
    MeasurementType.NitrogenDioxide, MeasurementType.Ozone -> null
  }

  DataSource.Government -> when (type) {
    MeasurementType.ParticulateMatter -> "Government-PM2.5"
    MeasurementType.NitrogenDioxide -> "Government-NO2"
    MeasurementType.Ozone -> "Government-Ozone"
    MeasurementType.Temperature, MeasurementType.Humidity -> null
  }
}

/** The filter dialog's selection. Null [source] / empty [measurementTypes] = "Show all". */
data class ExploreFilters(
  val source: DataSource? = null,
  val measurementTypes: Set<MeasurementType> = emptySet(),
) {
  /** The one type stations are coloured by; null unless exactly one is selected (only then does the server send a value). */
  val colourType: MeasurementType? get() = measurementTypes.singleOrNull()
  /** Parameter chips are multi-select: tapping one flips it. */
  fun toggled(type: MeasurementType): ExploreFilters =
    copy(measurementTypes = if (type in measurementTypes) measurementTypes - type else measurementTypes + type)
}

/** "Show N results": a station counts once, a cluster as its members. */
fun List<MapItem>.resultCount(): Int = sumOf {
  when (it) {
    is MapCluster -> it.count
    is MapStation -> 1
  }
}

fun MapStation.levelFor(
  type: MeasurementType,
  thresholds: Map<String, SensorThreshold>
): MeasurementLevel? {
  val value = valueFor(type) ?: return null
  val sensorName = source.sensorNameFor(type) ?: return null
  return thresholds[sensorName]?.levelFor(value.toDouble())
}