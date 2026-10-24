package pl.llp.aircasting.data.local

import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.sensorNameFor
import pl.llp.aircasting.recording.streamsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefaultThresholdsTest {

  private val thresholds = LocalThresholdsRepository(testDatabase().sensorThresholdQueries)

  @Test
  fun freshDatabaseHasServerDefaults() = runTest {
    assertEquals(SensorThreshold("AirBeam3-PM2.5", 0, 9, 35, 55, 150), thresholds.findBySensorName("AirBeam3-PM2.5"))
    assertEquals(SensorThreshold("Government-PM2.5", 0, 12, 35, 55, 150), thresholds.findBySensorName("Government-PM2.5"))
    assertEquals(SensorThreshold("AirBeam-F", 15, 45, 75, 105, 135), thresholds.findBySensorName("AirBeam-F"))
    assertEquals(SensorThreshold("Phone Microphone", 20, 60, 70, 80, 100), thresholds.findBySensorName("Phone Microphone"))
  }

  @Test
  fun everyRecordedStreamHasADefault() = runTest {
    val devices = listOf(AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1, AirBeamDevice.Mini.V2)
    devices.flatMap(::streamsFor).forEach { stream ->
      assertNotNull(thresholds.findBySensorName(stream.sensorName), "no default for ${stream.sensorName}")
    }
  }

  @Test
  fun everyExploreSensorHasADefault() = runTest {
    DataSource.entries.forEach { source ->
      MeasurementType.entries.mapNotNull(source::sensorNameFor).forEach { name ->
        assertNotNull(thresholds.findBySensorName(name), "no default for $name")
      }
    }
  }
}
