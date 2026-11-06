package pl.llp.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

  @Test
  fun bands_span_the_bounds_low_to_high() = assertEquals(
    listOf(
      0..12 to MeasurementLevel.LOW,
      12..35 to MeasurementLevel.MEDIUM,
      35..55 to MeasurementLevel.HIGH,
      55..150 to MeasurementLevel.VERY_HIGH,
    ),
    pm25.bands,
  )

  @Test
  fun moving_a_bound_sets_it() =
    assertEquals(pm25.copy(medium = 40), pm25.moveBound(InnerBound.Medium, 40))

  @Test
  fun a_bound_stops_one_short_of_its_upper_neighbour() =
    assertEquals(pm25.copy(medium = 54), pm25.moveBound(InnerBound.Medium, 90)) // high = 55

  @Test
  fun a_bound_stops_one_short_of_its_lower_neighbour() =
    assertEquals(pm25.copy(low = 1), pm25.moveBound(InnerBound.Low, -10)) // veryLow = 0

  @Test
  fun the_nearest_bound_takes_the_drag() {
    assertEquals(InnerBound.Low, pm25.nearestBound(20)) // |20−12| < |20−35|
    assertEquals(InnerBound.High, pm25.nearestBound(140))
  }

  @Test
  fun bounds_list_min_to_max() = assertEquals(listOf(0, 12, 35, 55, 150), pm25.bounds)

  @Test
  fun ordered_whole_numbers_are_valid() =
    assertEquals(emptySet(), invalidBounds(listOf("0", "12", "35", "55", "150")))

  @Test
  fun a_blank_or_non_numeric_field_is_invalid() =
    assertEquals(setOf(1, 3), invalidBounds(listOf("0", "", "35", "4.5", "150")))

  @Test
  fun a_field_not_above_the_one_before_is_invalid() =
    assertEquals(setOf(2), invalidBounds(listOf("0", "12", "12", "55", "150")))

  @Test
  fun valid_fields_become_the_bounds_and_keep_the_sensor() =
    assertEquals(pm25.copy(veryLow = 5, veryHigh = 200), pm25.withBounds(listOf("5", "12", "35", "55", " 200 ")))

  @Test
  fun invalid_fields_give_nothing_to_save() =
    assertNull(pm25.withBounds(listOf("0", "60", "35", "55", "150")))
}