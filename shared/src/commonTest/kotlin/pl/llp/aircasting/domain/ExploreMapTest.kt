package pl.llp.aircasting.domain

import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ExploreMapTest {

  @Test
  fun level_comes_from_the_streams_own_sensor_through_aliases() {
    val thresholds = Thresholds(
      listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)),
      aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
    )
    val station = station(stream(MeasurementType.ParticulateMatter, 12, sensorName = "AirBeamMini-PM2.5"))

    // 12 > low (9), ≤ medium (35)
    assertEquals(MeasurementLevel.MEDIUM, station.levelFor(MeasurementType.ParticulateMatter, thresholds))
  }

  @Test
  fun value_for_is_the_reading_of_that_types_stream() {
    val station = station(stream(MeasurementType.ParticulateMatter, 12), stream(MeasurementType.Temperature, 71))

    assertEquals(71, station.valueFor(MeasurementType.Temperature))
    assertNull(station.valueFor(MeasurementType.Humidity))
  }

  @Test
  fun level_is_null_when_there_is_nothing_to_colour() {
    val thresholds = Thresholds(listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)), emptyMap())

    assertNull(station().levelFor(MeasurementType.ParticulateMatter, thresholds)) // no PM stream
    assertNull(station(stream(MeasurementType.ParticulateMatter, 12)).levelFor(MeasurementType.ParticulateMatter, Thresholds(emptyList(), emptyMap()))) // no threshold row
  }

  @Test
  fun colour_type_only_when_exactly_one_type_is_selected() {
    assertNull(ExploreFilters().colourType)
    assertEquals(MeasurementType.Ozone, ExploreFilters(measurementTypes = setOf(MeasurementType.Ozone)).colourType)
    assertNull(ExploreFilters(measurementTypes = setOf(MeasurementType.Ozone, MeasurementType.Humidity)).colourType)
  }

  @Test
  fun toggling_a_type_adds_it_then_removes_it() {
    val pm = ExploreFilters().toggled(MeasurementType.ParticulateMatter)
    assertEquals(setOf(MeasurementType.ParticulateMatter), pm.measurementTypes)

    val pmOzone = pm.toggled(MeasurementType.Ozone)
    assertEquals(setOf(MeasurementType.ParticulateMatter, MeasurementType.Ozone), pmOzone.measurementTypes)

    assertEquals(setOf(MeasurementType.Ozone), pmOzone.toggled(MeasurementType.ParticulateMatter).measurementTypes)
  }

  @Test
  fun result_count_counts_a_station_once_and_a_cluster_as_its_members() {
    val station = station()
    val items = listOf(station, MapCluster(GeoLocation(1.0, 1.0), count = 37), station)

    assertEquals(39, items.resultCount())
  }

  private fun stream(type: MeasurementType, value: Int, sensorName: String = "AirBeam-PM2.5", unitSymbol: String = "µg/m³") =
    MapStream(
      id = 1, type = type, sensorName = sensorName, unitSymbol = unitSymbol, value = value,
      measuredAt = Instant.fromEpochMilliseconds(0),
    )

  private fun station(vararg streams: MapStream) = MapStation(
    uuid = "u1", source = DataSource.AirBeam, title = "Roof", provider = "jane",
    location = GeoLocation(40.1, -73.2), streams = streams.toList(),
  )
}
