package com.lunarlogic.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SensorThresholdTest {

  private val pm25 = SensorThreshold(
    sensorName = "government-pm2.5",
    veryLow = 0, low = 12, medium = 35, high = 55, veryHigh = 150,
  )

  @Test
  fun below_very_low_is_extremely_low() =
    assertEquals(MeasurementLevel.EXTREMELY_LOW, pm25.levelFor(-1.0))

  @Test
  fun inside_very_low_to_low_is_low() =
    assertEquals(MeasurementLevel.LOW, pm25.levelFor(5.0))

  @Test
  fun on_low_boundary_is_low() =
    assertEquals(MeasurementLevel.LOW, pm25.levelFor(12.0))

  @Test
  fun just_above_low_is_medium() =
    assertEquals(MeasurementLevel.MEDIUM, pm25.levelFor(20.0))

  @Test
  fun on_medium_boundary_is_medium() =
    assertEquals(MeasurementLevel.MEDIUM, pm25.levelFor(35.0))

  @Test
  fun above_medium_is_high() =
    assertEquals(MeasurementLevel.HIGH, pm25.levelFor(40.0))

  @Test
  fun above_high_is_very_high() =
    assertEquals(MeasurementLevel.VERY_HIGH, pm25.levelFor(100.0))

  @Test
  fun above_very_high_is_extremely_high() =
    assertEquals(MeasurementLevel.EXTREMELY_HIGH, pm25.levelFor(200.0))

  @Test
  fun rounds_before_banding() {
    assertEquals(MeasurementLevel.LOW, pm25.levelFor(12.4))
    assertEquals(MeasurementLevel.MEDIUM, pm25.levelFor(12.6))
  }
}