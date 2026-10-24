package pl.llp.aircasting.data.network

import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MeasurementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ExploreMapMapperTest {
  private fun station(
    source: String = "airbeam",
    measurementTypes: List<String> = listOf("Particulate Matter"),
  ) = ExploreMapStationDto(
    uuid = "u1", source = source, title = "Roof", provider = "jane",
    latitude = 40.1, longitude = -73.2, lastMeasuredAt = 1792659600000,
    measurementTypes = measurementTypes, value = 12,
  )

  @Test
  fun maps_cluster_to_location_and_count() {
    val items = ExploreMapResponseDto(listOf(ExploreMapClusterDto(40.7, -73.9, count = 37, value = 5)))
      .toMapItems()

    assertEquals(listOf(MapCluster(GeoLocation(40.7, -73.9), count = 37)), items)
  }
  @Test
  fun maps_station_fields_to_domain_types() {
    val items = ExploreMapResponseDto(listOf(station())).toMapItems()

    assertEquals(
      listOf(
        MapStation(
          uuid = "u1", source = DataSource.AirBeam, title = "Roof", provider = "jane",
          location = GeoLocation(40.1, -73.2),
          lastMeasuredAt = Instant.fromEpochMilliseconds(1792659600000),
          measurementTypes = listOf(MeasurementType.ParticulateMatter),
          value = 12,
        )
      ),
      items,
    )
  }
  @Test
  fun drops_measurement_types_this_app_does_not_know() {
    val dto = station(measurementTypes = listOf("Ozone", "Carbon Monoxide"))

    val item = ExploreMapResponseDto(listOf(dto)).toMapItems().single() as MapStation

    assertEquals(listOf(MeasurementType.Ozone), item.measurementTypes)
  }

  @Test
  fun drops_stations_from_an_unknown_source() {
    val items = ExploreMapResponseDto(listOf(station(source = "purpleair"), station())).toMapItems()

    assertEquals(1, items.size)
  }
}
