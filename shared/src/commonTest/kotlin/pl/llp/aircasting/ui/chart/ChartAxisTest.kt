package pl.llp.aircasting.ui.chart

import pl.llp.aircasting.domain.SensorThreshold
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals

class ChartAxisTest {

  // 2026-10-27T01:00:00Z — a local-as-UTC epoch whose numerals are the station's 01:00.
  private val oneAm = 1_793_062_800.0

  @Test
  fun times_are_formatted_as_UTC_so_the_station_wall_clock_shows_unchanged() {
    // Never converted to the device zone: that would shift a New York station by the viewer's offset.
    assertEquals("01:00", hourMinuteUtc(oneAm))
    assertEquals("01:05", hourMinuteUtc(oneAm + 5 * 60 + 59)) // seconds are dropped, not rounded
  }

  @Test
  fun tooltip_time_includes_seconds() {
    assertEquals("01:05:59", hourMinuteSecondUtc(oneAm + 5 * 60 + 59))
  }

  @Test
  fun hour_labels_start_at_the_first_full_hour() {
    assertEquals(0, secondsToNextHour(oneAm))
    assertEquals(3_600 - 90, secondsToNextHour(oneAm + 90))
  }

  @Test
  fun with_a_threshold_the_y_axis_spans_its_bands() {
    val points = listOf(ChartPoint(0.0, 3.0), ChartPoint(1.0, 40.0))

    assertEquals(0.0..150.0, yRange(points, PM))
  }

  @Test
  fun a_reading_above_the_top_band_widens_the_y_axis_so_it_is_not_cut_off() {
    val points = listOf(ChartPoint(0.0, 3.0), ChartPoint(1.0, 210.0))

    assertEquals(0.0..210.0, yRange(points, PM))
  }

  @Test
  fun without_a_threshold_the_y_axis_spans_the_data() {
    val points = listOf(ChartPoint(0.0, 3.0), ChartPoint(1.0, 40.0))

    assertEquals(3.0..40.0, yRange(points, threshold = null))
  }

  @Test
  fun a_flat_line_without_a_threshold_still_gets_a_non_empty_y_axis() {
    val points = listOf(ChartPoint(0.0, 7.0), ChartPoint(1.0, 7.0))

    assertEquals(6.0..8.0, yRange(points, threshold = null))
  }

  @Test
  fun a_window_crossing_midnight_gets_one_line_at_00_00() {
    val sevenPm = oneAm - 6 * 3_600 // 2026-10-26T19:00
    val midnight = oneAm - 3_600

    assertEquals(listOf(midnight), midnightsBetween(sevenPm, sevenPm + 24 * 3_600))
  }

  @Test
  fun a_window_within_one_day_gets_no_line() {
    assertEquals(emptyList(), midnightsBetween(oneAm, oneAm + 10 * 3_600))
  }
  @Test
  fun a_series_starting_exactly_at_midnight_gets_no_line_on_its_left_edge() {
    // Legacy draws the line only once the day *changes* from the first point's day.
    val midnight = oneAm - 3_600

    assertEquals(emptyList(), midnightsBetween(midnight, midnight + 5 * 3_600))
  }
}

private val PM = SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)
