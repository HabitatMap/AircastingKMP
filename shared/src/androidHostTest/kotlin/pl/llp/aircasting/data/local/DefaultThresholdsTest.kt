package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.recording.streamsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefaultThresholdsTest {

  private val repository = LocalThresholdsRepository(testDatabase().sensorThresholdQueries)
  private val pm25 = SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)

  private suspend fun forSensor(name: String) = repository.observe().first().forSensor(name)

  /** Seed mirrors GET /api/v3/threshold_sets as of 2026-11-04. */
  @Test
  fun freshDatabaseHasServerDefaults() = runTest {
    assertEquals(pm25, forSensor("AirBeam3-PM2.5"))
    assertEquals(pm25, forSensor("Government-PM2.5")) // server alias of AirBeam-PM2.5
    assertEquals(SensorThreshold("AirBeam-F", 15, 45, 75, 105, 135), forSensor("AirBeam-F"))
    assertEquals(SensorThreshold("Phone Microphone", 20, 60, 70, 80, 100), forSensor("Phone Microphone"))
  }

  @Test
  fun everyAirBeamModelSharesOneThreshold() = runTest {
    listOf("AirBeam-PM", "AirBeam2-PM2.5", "AirBeam3-PM2.5", "AirBeamMini-PM2.5").forEach {
      assertEquals(pm25, forSensor(it), it)
    }
  }

  @Test
  fun everyRecordedStreamHasADefault() = runTest {
    val devices = listOf(AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1, AirBeamDevice.Mini.V2)
    devices.flatMap(::streamsFor).forEach { stream ->
      assertNotNull(forSensor(stream.sensorName), "no default for ${stream.sensorName}")
    }
  }

  @Test
  fun everyExploreSensorHasADefault() = runTest {
    // BE Explore::Parameters::CATALOGUE (docs/API_V3.md → Explore map)
    listOf("AirBeam-PM2.5", "AirBeam-F", "AirBeam-RH", "Government-PM2.5", "Government-NO2", "Government-Ozone")
      .forEach { assertNotNull(forSensor(it), "no default for $it") }
  }

  @Test
  fun anOverrideReachesEveryModelButNotOtherParameters() = runTest {
    repository.save(pm25.copy(low = 20))

    assertEquals(20, forSensor("AirBeamMini-PM2.5")?.low)
    assertEquals(20, forSensor("AirBeam3-PM2.5")?.low)
    assertEquals(9, forSensor("AirBeamMini-PM1")?.low)
  }

  @Test
  fun resetToDefaultDropsTheOverride() = runTest {
    repository.save(pm25.copy(low = 20))

    repository.resetToDefault("AirBeam-PM2.5")

    assertEquals(pm25, forSensor("AirBeamMini-PM2.5"))
  }
}