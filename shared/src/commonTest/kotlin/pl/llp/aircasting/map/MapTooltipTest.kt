package pl.llp.aircasting.map

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.settings.app.RegionalFormat
import pl.llp.aircasting.ui.format.DateFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MapTooltipTest {
  private val reading = Instant.parse("2026-08-05T10:15:30Z")
  private val warsaw = TimeZone.of("Europe/Warsaw")

  @Test
  fun shows_the_reading_time_on_the_sessions_wall_clock_to_the_second() {
    // Warsaw is UTC+2 in August: the device's own zone must not leak in.
    assertEquals("12:15:30", tooltipTime(reading, warsaw, DateFormatter(RegionalFormat.UK)))
  }

  @Test
  fun follows_the_regional_clock() {
    assertEquals("12:15:30 PM", tooltipTime(reading, warsaw, DateFormatter(RegionalFormat.US)))
  }
}