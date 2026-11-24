package pl.llp.aircasting.data.network

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Our own fixed session's details and graph, read from the authenticated /fixed_sessions endpoints. */
class FixedSessionDetailsTest {
  private val requests = mutableListOf<HttpRequestData>()
  private val auth = StoredAuthSession(MapSettings()).apply { start("user-token", AccountProfile("u", "u@example.com")) }

  @Test
  fun details_come_from_the_session_endpoint_with_the_user_token() = runTest {
    details(OUTDOOR_JSON).station("f1")

    assertEquals("/api/v3/fixed_sessions/f1", requests.single().url.encodedPath)
    assertEquals("Bearer user-token", requests.single().headers[HttpHeaders.Authorization])
  }

  @Test
  fun maps_to_station_details_with_streams_in_chip_order() = runTest {
    assertEquals(
      StationDetails(
        uuid = "f1",
        type = SessionType.FIXED,
        source = DataSource.AirBeam,
        title = "Home AirBeam Garden",
        provider = "",
        deviceModel = "AirBeamMini",
        timeZone = TimeZone.of("America/New_York"),
        streams = listOf(
          StationStream(0, "AirBeamMini-PM1", "µg/m³", value = 3, min = null, max = null, measuredAt = MEASURED_AT),
          StationStream(1, "AirBeamMini-PM2.5", "µg/m³", value = 8, min = null, max = null, measuredAt = MEASURED_AT),
        ),
        tags = listOf("Home", "Alaska"),
        isIndoor = false,
        location = GeoLocation(40.1, -73.2),
        startTime = START,
        endTime = null,
      ),
      details(OUTDOOR_JSON).station("f1"),
    )
  }

  @Test
  fun an_indoor_session_has_no_location_to_map() = runTest {
    val json = OUTDOOR_JSON.replace(""""is_indoor":false""", """"is_indoor":true""")
      .replace(""""latitude":40.1,"longitude":-73.2""", """"latitude":200,"longitude":200""")

    val station = details(json).station("f1")

    assertEquals(true, station.isIndoor)
    assertNull(station.location)
  }

  @Test
  fun graph_reads_two_12h_windows_from_the_fixed_session_measurements() = runTest {
    val newest = MEASURED_AT
    fixedSessionMeasurements(api("[]"), auth).measurements("f1", "AirBeamMini-PM2.5", newest, TimeZone.UTC)

    assertEquals(setOf("/api/v3/fixed_sessions/f1/measurements"), requests.map { it.url.encodedPath }.toSet())
    assertEquals(setOf("Bearer user-token"), requests.map { it.headers[HttpHeaders.Authorization] }.toSet())
    assertEquals(
      (newest - 24.hours).toEpochMilliseconds(),
      requests.minOf { it.url.parameters["start_time"]!!.toLong() },
    )
  }

  private fun details(json: String) = NetworkFixedSessionDetailsRepository(api(json), auth)

  private fun api(json: String) = FixedSessionsApi(
    createAircastingHttpClient(
      MockEngine { request ->
        requests += request
        respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
      },
    ),
  )

  private companion object {
    val START: Instant = Instant.fromEpochMilliseconds(1786663800000)
    val MEASURED_AT: Instant = Instant.fromEpochMilliseconds(1786706940000)

    // Streams deliberately out of chip order: the server keys them by name.
    const val OUTDOOR_JSON = """
{"uuid":"f1","title":"Home AirBeam Garden","type":"FixedSession","tag_list":"Home Alaska","contribute":true,
 "is_indoor":false,"time_zone":"America/New_York","start_time":1786663800000,"end_time":1786707000000,
 "last_measurement_at":1786707000000,"finished_at":null,"version":1,"latitude":40.1,"longitude":-73.2,
 "share_url":"https://aircasting.org/s/ab12c","device":{"mac_address":"aa:bb","model":"AirBeamMini"},
 "streams":{
  "AirBeamMini-PM2.5":{"sensor_name":"AirBeamMini-PM2.5","unit_symbol":"µg/m³","last_measurement":{"value":8.4,"time":1786706940000}},
  "AirBeamMini-PM1":{"sensor_name":"AirBeamMini-PM1","unit_symbol":"µg/m³","last_measurement":{"value":2.6,"time":1786706940000}}}}
"""
  }
}
