package pl.llp.aircasting.explore

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.i18n.EnStrings
import pl.llp.aircasting.settings.app.RegionalFormat
import pl.llp.aircasting.ui.format.DateFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class DetailsStatsTest {
  private val start = Instant.parse("2026-11-23T12:34:00Z")
  private val dates = DateFormatter(RegionalFormat.UK)
  private val stream = StationStream(1, "AirBeamMini-PM2.5", "µg/m³", value = 7, min = 3, max = 40, measuredAt = start, avg = 12)

  private fun details(
    startTime: Instant? = null, endTime: Instant? = null, streams: List<StationStream> = listOf(stream),
  ) = StationDetails(
    uuid = "u", type = SessionType.MOBILE, source = DataSource.AirBeam, title = "Walk", provider = "",
    deviceModel = "AirBeamMini", timeZone = TimeZone.of("Europe/Warsaw"), streams = streams,
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
  fun own_fixed_session_shows_only_now_until_the_server_sends_stats() {
    assertEquals(
      listOf(DetailsStat("NOW", 7)),
      details(startTime = start).copy(type = SessionType.FIXED).stats(stream, EnStrings),
    )
  }

  @Test
  fun finished_own_fixed_session_shows_empty_avg_min_max_until_the_server_sends_them() {
    assertEquals(
      listOf(DetailsStat("AVG.", null), DetailsStat("MIN.", null), DetailsStat("MAX.", null)),
      details(startTime = start, endTime = start + 6.hours).copy(type = SessionType.FIXED).stats(stream, EnStrings),
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
  fun live_session_runs_from_its_start_to_its_newest_measurement_across_streams() {
    val streams = listOf(
      stream.copy(id = 1, measuredAt = start + 2.hours),
      stream.copy(id = 2, measuredAt = start + 3.hours),
      stream.copy(id = 3, measuredAt = null),
    )
    assertEquals("23/11/26, 13:34 - 23/11/26, 16:34", details(startTime = start, streams = streams).timeRange(dates))
  }

  @Test
  fun finished_session_ends_at_its_last_measurement_not_at_the_finish_tap() {
    val streams = listOf(stream.copy(measuredAt = start + 5.hours))
    assertEquals(
      "23/11/26, 13:34 - 23/11/26, 18:34",
      details(start, endTime = start + 6.hours, streams = streams).timeRange(dates),
    )
  }

  @Test
  fun session_without_measurements_shows_only_its_start() {
    assertEquals("23/11/26, 13:34", details(startTime = start, streams = listOf(stream.copy(measuredAt = null))).timeRange(dates))
  }

  @Test
  fun explore_station_has_no_time_range() {
    assertNull(details().timeRange(dates))
  }
}
