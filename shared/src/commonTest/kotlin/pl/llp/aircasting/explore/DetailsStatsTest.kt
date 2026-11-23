package pl.llp.aircasting.explore

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DetailsStatsTest {
  private val start = Instant.parse("2026-11-23T12:34:00Z")
  private val stream = StationStream(1, "AirBeamMini-PM2.5", "µg/m³", value = 7, min = 3, max = 40, measuredAt = start, avg = 12)

  private fun details(startTime: Instant? = null, endTime: Instant? = null) = StationDetails(
    uuid = "u", type = SessionType.MOBILE, source = DataSource.AirBeam, title = "Walk", provider = "",
    deviceModel = "AirBeamMini", timeZone = TimeZone.of("Europe/Warsaw"), streams = listOf(stream),
    startTime = startTime, endTime = endTime,
  )

  @Test
  fun explore_station_shows_now_and_the_last_24h() {
    assertEquals(
      listOf(DetailsStat("NOW", 7), DetailsStat("MIN. 24H", 3), DetailsStat("MAX. 24H", 40)),
      details().stats(stream, EnStrings),
    )
  }

  @Test
  fun live_session_shows_now_and_whole_session_avg_min_max() {
    assertEquals(
      listOf(DetailsStat("NOW", 7), DetailsStat("AVG.", 12), DetailsStat("MIN.", 3), DetailsStat("MAX.", 40)),
      details(startTime = start).stats(stream, EnStrings),
    )
  }

  @Test
  fun finished_session_has_no_now() {
    assertEquals(
      listOf(DetailsStat("AVG.", 12), DetailsStat("MIN.", 3), DetailsStat("MAX.", 40)),
      details(startTime = start, endTime = start + 6.hours).stats(stream, EnStrings),
    )
  }

  @Test
  fun finished_session_shows_its_range_on_its_own_wall_clock() {
    assertEquals("23/11/26, 13:34 - 23/11/26, 19:34", details(start, start + 6.hours).timeRange())
  }

  @Test
  fun live_session_shows_only_its_start_like_legacy_DurationStringHelper() {
    assertEquals("23/11/26, 13:34", details(startTime = start).timeRange())
  }

  @Test
  fun explore_station_has_no_time_range() {
    assertNull(details().timeRange())
  }
}
