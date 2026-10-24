package pl.llp.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ExploreMapTest {

  @Test
  fun airbeam_sensor_per_measurement_type() {
    assertEquals("AirBeam-PM2.5", DataSource.AirBeam.sensorNameFor(MeasurementType.ParticulateMatter))
    assertEquals("AirBeam-F", DataSource.AirBeam.sensorNameFor(MeasurementType.Temperature))
    assertEquals("AirBeam-RH", DataSource.AirBeam.sensorNameFor(MeasurementType.Humidity))
    assertNull(DataSource.AirBeam.sensorNameFor(MeasurementType.NitrogenDioxide))
    assertNull(DataSource.AirBeam.sensorNameFor(MeasurementType.Ozone))
  }
  @Test
  fun government_sensor_per_measurement_type() {
    assertEquals("Government-PM2.5", DataSource.Government.sensorNameFor(MeasurementType.ParticulateMatter))
    assertEquals("Government-NO2", DataSource.Government.sensorNameFor(MeasurementType.NitrogenDioxide))
    assertEquals("Government-Ozone", DataSource.Government.sensorNameFor(MeasurementType.Ozone))
    assertNull(DataSource.Government.sensorNameFor(MeasurementType.Temperature))
    assertNull(DataSource.Government.sensorNameFor(MeasurementType.Humidity))
  }

  @Test
  fun level_is_null_when_there_is_nothing_to_colour() {
    val thresholds = mapOf("AirBeam-PM2.5" to SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150))
    val station = MapStation(
      uuid = "u1", source = DataSource.AirBeam, title = "Roof", provider = "jane",
      location = GeoLocation(40.1, -73.2), lastMeasuredAt = Instant.fromEpochMilliseconds(0),
      measurementTypes = listOf(MeasurementType.ParticulateMatter), value = 12,
    )

    assertNull(station.copy(value = null).levelFor(MeasurementType.ParticulateMatter, thresholds))
    assertNull(station.levelFor(MeasurementType.ParticulateMatter, emptyMap()))
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
    val station = MapStation(
      uuid = "u", source = DataSource.AirBeam, title = "t", provider = "p",
      location = GeoLocation(0.0, 0.0), lastMeasuredAt = Instant.fromEpochMilliseconds(0),
      measurementTypes = emptyList(), value = null,
    )
    val items = listOf(station, MapCluster(GeoLocation(1.0, 1.0), count = 37), station)

    assertEquals(39, items.resultCount())
  }
}
