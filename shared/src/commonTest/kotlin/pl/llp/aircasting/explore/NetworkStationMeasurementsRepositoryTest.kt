package pl.llp.aircasting.explore

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class NetworkStationMeasurementsRepositoryTest {

  @Test
  fun asks_publicly_for_two_12h_windows_back_from_the_newest_reading() = runTest {
    val requests = mutableListOf<HttpRequestData>()
    repository { requests += it; "[]" }.measurements("87d8", "Government-Ozone", NEWEST, TimeZone.UTC)

    assertEquals(setOf("/api/v3/explore/stations/87d8/measurements"), requests.map { it.url.encodedPath }.toSet())
    assertEquals(listOf("Government-Ozone", "Government-Ozone"), requests.map { it.url.parameters["sensor_name"] })
    val split = NEWEST - 12.hours
    assertEquals(
      setOf(
        split.toEpochMilliseconds() to NEWEST.toEpochMilliseconds(),
        // Both bounds are inclusive server-side: stop 1 ms short so the boundary reading isn't doubled.
        (NEWEST - 24.hours).toEpochMilliseconds() to split.toEpochMilliseconds() - 1,
      ),
      requests.map { it.url.parameters["start_time"]!!.toLong() to it.url.parameters["end_time"]!!.toLong() }.toSet(),
    )
    assertTrue(requests.none { it.headers[HttpHeaders.Authorization] != null })
  }

  @Test
  fun returns_both_windows_on_the_stations_wall_clock() = runTest {
    val repo = repository { request ->
      if (request.url.parameters["end_time"] == NEWEST.toEpochMilliseconds().toString()) {
        """[{"time":1793887200000,"value":19.0}]"""
      } else {
        """[{"time":1793840400000,"value":29.0}]"""
      }
    }

    val points = repo.measurements("87d8", "Government-Ozone", NEWEST, TimeZone.of("America/New_York"))

    // 14:00 and 01:00 UTC = 09:00 and (the day before) 20:00 in New York, EST (UTC−5), packed as UTC.
    assertEquals(setOf(ChartPoint(1793869200.0, 19.0), ChartPoint(1793822400.0, 29.0)), points.toSet())
  }

  private fun repository(body: (HttpRequestData) -> String): StationMeasurementsRepository {
    val engine = MockEngine { request ->
      respond(body(request), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return NetworkStationMeasurementsRepository(ExploreMapApi(createAircastingHttpClient(engine)))
  }
}

/** Leonia (gov, America/New_York) Government-Ozone last reading, live 2026-11-05. */
private val NEWEST = Instant.fromEpochMilliseconds(1793887200000)
