package pl.llp.aircasting.domain

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

/** One AirBeam fixed session or government station. [value] only when exactly one type is selected. */
data class MapStation(
  val uuid: String,
  val source: DataSource,
  val title: String,
  val provider: String,
  override val location: GeoLocation,
  val lastMeasuredAt: Instant,
  val measurementTypes: List<MeasurementType>,
  val value: Int?,
) : MapItem

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
}

/**
 * Colour of this station's [value] for [type], from the user's [thresholds] keyed by
 * sensor name. Null = nothing to colour (no value, or no threshold row).
 */
fun MapStation.levelFor(
  type: MeasurementType,
  thresholds: Map<String, SensorThreshold>
): MeasurementLevel? {
  val value = value ?: return null
  val sensorName = source.sensorNameFor(type) ?: return null
  return thresholds[sensorName]?.levelFor(value.toDouble())
}