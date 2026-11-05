package pl.llp.aircasting.data.network

import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapItem
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapStream
import pl.llp.aircasting.domain.MeasurementType
import kotlin.time.Instant

fun ExploreMapResponseDto.toMapItems(): List<MapItem> = items.mapNotNull { it.toMapItem() }

/** Null = a station from a source this app doesn't know; skipped rather than failing the map. */
private fun ExploreMapItemDto.toMapItem(): MapItem? {
  val location = GeoLocation(latitude, longitude)
  return when (this) {
    is ExploreMapClusterDto -> MapCluster(location, count)
    is ExploreMapStationDto -> {
      val source = DataSource.entries.find { it.apiName == source } ?: return null
      MapStation(uuid, source, title, provider, location, streams.mapNotNull { it.toMapStream() })
    }
  }
}

/** Null = a measurement type this app doesn't know; dropped, the rest of the station stays. */
private fun ExploreMapStreamDto.toMapStream(): MapStream? {
  val type = MeasurementType.entries.find { it.apiName == measurementType } ?: return null
  return MapStream(streamId, type, sensorName, unitSymbol, lastMeasurement.value, Instant.fromEpochMilliseconds(lastMeasurement.time))
}