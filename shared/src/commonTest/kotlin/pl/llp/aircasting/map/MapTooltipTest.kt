package pl.llp.aircasting.map

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MapTooltipTest {
  @Test
  fun shows_the_reading_time_on_the_sessions_wall_clock_to_the_second() {
    // Warsaw is UTC+2 in August: the device's own zone must not leak in.
    assertEquals("12:15:30", tooltipTime(Instant.parse("2026-08-05T10:15:30Z"), TimeZone.of("Europe/Warsaw")))
  }
}