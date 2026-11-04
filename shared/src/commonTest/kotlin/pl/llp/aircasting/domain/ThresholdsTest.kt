package pl.llp.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThresholdsTest {

  private val pm1 = SensorThreshold("AirBeam-PM1", 0, 9, 35, 55, 150)
  private val pm25 = SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)
  private val aliases = mapOf(
    "AirBeamMini-PM1" to "AirBeam-PM1",
    "AirBeamMini-PM2.5" to "AirBeam-PM2.5",
    "AirBeam3-PM2.5" to "AirBeam-PM2.5",
  )

  @Test
  fun a_model_spelling_resolves_to_its_parameters_default() =
    assertEquals(pm1, Thresholds(listOf(pm1, pm25), aliases).forSensor("AirBeamMini-PM1"))

  @Test
  fun the_canonical_name_resolves_without_an_alias() =
    assertEquals(pm25, Thresholds(listOf(pm1, pm25), emptyMap()).forSensor("AirBeam-PM2.5"))
  @Test
  fun an_override_applies_to_every_model_of_that_parameter_only() {
    val edited = pm25.copy(low = 20)
    val thresholds = Thresholds(listOf(pm1, pm25), aliases, overrides = listOf(edited))

    assertEquals(edited, thresholds.forSensor("AirBeamMini-PM2.5"))
    assertEquals(edited, thresholds.forSensor("AirBeam3-PM2.5"))
    assertEquals(pm1, thresholds.forSensor("AirBeamMini-PM1"))
  }

  @Test
  fun an_unknown_sensor_has_no_threshold() =
    assertNull(Thresholds(listOf(pm1), aliases).forSensor("AirBeam3-CO2"))
}