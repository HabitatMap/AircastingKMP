package pl.llp.aircasting.ui.format

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.settings.app.RegionalFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DateFormatterTest {
  private val warsaw = TimeZone.of("Europe/Warsaw")

  // 2026-11-03 09:05 in Warsaw (UTC+1): single-digit day, month-like hour, to catch padding/order.
  private val start = Instant.parse("2026-11-03T08:05:00Z")

  @Test
  fun each_regional_format_orders_and_separates_the_date_its_own_way() {
    mapOf(
      RegionalFormat.US to "11/03/26, 9:05 AM",
      RegionalFormat.UK to "03/11/26, 09:05",
      RegionalFormat.CentralEuropean to "03.11.26, 09:05",
      RegionalFormat.Nordic to "03.11.26, 09:05",
      RegionalFormat.EastAsian to "26/11/03, 09:05",
    ).forEach { (format, expected) ->
      assertEquals(expected, DateFormatter(format).dateTime(start, warsaw), "$format")
    }
  }

  @Test
  fun us_uses_a_12_hour_clock_with_no_leading_zero_and_the_rest_24_hours() {
    val afternoon = start + 4.hours + 29.minutes // 13:34 in Warsaw
    assertEquals("11/03/26, 1:34 PM", DateFormatter(RegionalFormat.US).dateTime(afternoon, warsaw))
    assertEquals("03/11/26, 13:34", DateFormatter(RegionalFormat.UK).dateTime(afternoon, warsaw))
  }

  @Test
  fun us_midnight_and_noon_read_as_12() {
    val midnight = Instant.parse("2026-11-03T00:00:00Z")
    assertEquals("11/03/26, 12:00 AM", DateFormatter(RegionalFormat.US).dateTime(midnight, TimeZone.UTC))
    assertEquals("11/03/26, 12:00 PM", DateFormatter(RegionalFormat.US).dateTime(midnight + 12.hours, TimeZone.UTC))
  }

  @Test
  fun time_with_seconds_follows_the_regional_clock() {
    val afternoon = start + 4.hours + 29.minutes + 7.seconds // 13:34:07 in Warsaw
    assertEquals("1:34:07 PM", DateFormatter(RegionalFormat.US).timeWithSeconds(afternoon, warsaw))
    assertEquals("13:34:07", DateFormatter(RegionalFormat.UK).timeWithSeconds(afternoon, warsaw))
    assertEquals("09:05:00", DateFormatter(RegionalFormat.CentralEuropean).timeWithSeconds(start, warsaw))
  }

  @Test
  fun range_over_midnight_shows_both_dates() {
    assertEquals(
      "03/11/26, 09:05 - 04/11/26, 08:10",
      DateFormatter(RegionalFormat.UK).range(start, start + 23.hours + 5.minutes, warsaw),
    )
  }

  @Test
  fun range_without_an_end_is_just_the_start() {
    assertEquals("03/11/26, 09:05", DateFormatter(RegionalFormat.UK).range(start, end = null, warsaw))
  }

  @Test
  fun times_are_on_the_given_zones_wall_clock() {
    assertEquals("03/11/26, 08:05", DateFormatter(RegionalFormat.UK).dateTime(start, TimeZone.UTC))
  }
}
