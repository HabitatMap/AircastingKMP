package pl.llp.aircasting.domain

import kotlin.time.Instant

/**
 * The `measurement_type` catalogue the Explore map filters by. [apiName] is the wire value.
 * Units and sensors come per stream from the server, not from here.
 */
enum class MeasurementType(val apiName: String) {
  ParticulateMatter("Particulate Matter"),
  NitrogenDioxide("Nitrogen Dioxide"),
  Ozone("Ozone"),
  Temperature("Temperature"),
  Humidity("Humidity"),
}

enum class ExploreSource(val apiName: String) {
  AirBeam("airbeam"),
  Government("government"),
}

/**
 * What the map is showing: visible bounds + the map SDK's fractional zoom, and the camera [centre] —
 * reported by the SDK rather than derived from [bounds], whose Mercator midpoint isn't the centre.
 */
data class MapViewport(val bounds: GeoSquare, val zoom: Double, val centre: GeoLocation)

/** A marker on the Explore map. */
sealed interface MapItem {
  val location: GeoLocation
}

/** Several items sharing a server grid cell. Never colour-coded; zooming in splits it. */
data class MapCluster(override val location: GeoLocation, val count: Int) : MapItem

/**
 * A station's reading for one selected [type]. [sensorName] keys the user's thresholds; [id] keys the
 * stream-detail endpoints, unique only within the station's source.
 */
data class MapStream(
  val id: Long,
  val type: MeasurementType,
  val sensorName: String,
  val unitSymbol: String,
  val value: Int,
  val measuredAt: Instant,
)

/** One AirBeam fixed session or government station, with a stream per selected type it reports. */
data class MapStation(
  val uuid: String,
  val source: ExploreSource,
  val title: String,
  val provider: String,
  override val location: GeoLocation,
  val streams: List<MapStream>,
) : MapItem {
  /** This station's stream for [type]; null when it doesn't report it. */
  fun streamFor(type: MeasurementType): MapStream? = streams.find { it.type == type }
  fun valueFor(type: MeasurementType): Int? = streamFor(type)?.value
}

/** Coloured by the stream's own sensor (any model spelling resolves through [Thresholds]' aliases). */
fun MapStation.levelFor(type: MeasurementType, thresholds: Thresholds): MeasurementLevel? {
  val stream = streamFor(type) ?: return null
  return thresholds.forSensor(stream.sensorName)?.levelFor(stream.value.toDouble())
}

/** The filter dialog's selection. Null [source] / empty [measurementTypes] = "Show all". */
data class ExploreFilters(
  val source: ExploreSource? = null,
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

