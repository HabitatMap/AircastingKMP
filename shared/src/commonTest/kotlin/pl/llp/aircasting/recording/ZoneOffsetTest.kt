package pl.llp.aircasting.recording

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/** The offset a V1 Wi-Fi config carries: legacy TimezoneHelper.getTimezoneOffsetInHours (rawOffset). */
class ZoneOffsetTest {

  @Test
  fun summer_time_is_left_out() {
    assertEquals(1, TimeZone.of("Europe/Warsaw").standardOffsetHours(2026))
  }

  /** Sydney's summer time runs over January: the July offset is the standard one. */
  @Test
  fun southern_summer_time_is_left_out_too() {
    assertEquals(10, TimeZone.of("Australia/Sydney").standardOffsetHours(2026))
  }

  /** Legacy converts with TimeUnit.HOURS, which truncates: +5:30 → 5, −3:30 → −3. */
  @Test
  fun half_hour_zones_lose_the_half_hour() {
    assertEquals(5, TimeZone.of("Asia/Kolkata").standardOffsetHours(2026))
    assertEquals(-3, TimeZone.of("America/St_Johns").standardOffsetHours(2026))
  }
}
