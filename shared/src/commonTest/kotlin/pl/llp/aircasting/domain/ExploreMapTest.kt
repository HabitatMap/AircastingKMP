package pl.llp.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
