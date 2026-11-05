package pl.llp.aircasting.data.network

import kotlinx.serialization.decodeFromString
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapStream
import pl.llp.aircasting.domain.MeasurementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExploreMapMapperTest {
  private fun stream(type: String = "Particulate Matter", id: Long = 4821) = ExploreMapStreamDto(
    streamId = id,
    sensorName = "AirBeam-PM2.5",
    measurementType = type,
    unitSymbol = "µg/m³",
    lastMeasurement = ExploreLastMeasurementDto(value = 12, time = 1792659600000),
  )

  private fun station(
    source: String = "airbeam",
    streams: List<ExploreMapStreamDto> = listOf(stream()),
  ) = ExploreMapStationDto(
    uuid = "u1", source = source, title = "Roof", provider = "jane",
    latitude = 40.1, longitude = -73.2, streams = streams,
  )

  @Test
  fun decodes_a_station_in_the_streams_shape() {
    val json = """
      {"items":[{"type":"station","uuid":"u1","source":"government","title":"Fort Lee","provider":"US EPA AirNow",
        "latitude":40.85,"longitude":-73.96,
        "streams":[{"stream_id":30027,"sensor_name":"Government-PM2.5","measurement_type":"Particulate Matter",
          "unit_symbol":"µg/m³","last_measurement":{"value":7,"time":1793282400000}}]}]}
    """.trimIndent()

    val dto = AircastingJson.decodeFromString<ExploreMapResponseDto>(json)

    assertEquals(30027, (dto.items.single() as ExploreMapStationDto).streams.single().streamId)
  }

  @Test
  fun maps_cluster_to_location_and_count() {
    val items = ExploreMapResponseDto(listOf(ExploreMapClusterDto(40.7, -73.9, count = 37, value = 5)))
      .toMapItems()

    assertEquals(listOf(MapCluster(GeoLocation(40.7, -73.9), count = 37)), items)
  }

  @Test
  fun maps_station_and_its_streams_to_domain_types() {
    val items = ExploreMapResponseDto(listOf(station())).toMapItems()

    assertEquals(
      listOf(
        MapStation(
          uuid = "u1", source = DataSource.AirBeam, title = "Roof", provider = "jane",
          location = GeoLocation(40.1, -73.2),
          streams = listOf(
            MapStream(
              id = 4821, type = MeasurementType.ParticulateMatter,
              sensorName = "AirBeam-PM2.5", unitSymbol = "µg/m³", value = 12,
              measuredAt = Instant.fromEpochMilliseconds(1792659600000),
            )
          ),
        )
      ),
      items,
    )
  }
  @Test
  fun drops_streams_of_measurement_types_this_app_does_not_know() {
    val dto = station(streams = listOf(stream("Ozone", id = 1), stream("Carbon Monoxide", id = 2)))

    val item = ExploreMapResponseDto(listOf(dto)).toMapItems().single() as MapStation

    assertEquals(listOf(MeasurementType.Ozone), item.streams.map { it.type })
  }

  @Test
  fun drops_stations_from_an_unknown_source() {
    val items = ExploreMapResponseDto(listOf(station(source = "purpleair"), station())).toMapItems()

    assertEquals(1, items.size)
  }
}