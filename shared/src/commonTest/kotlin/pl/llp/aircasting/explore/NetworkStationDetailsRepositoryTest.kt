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
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class NetworkStationDetailsRepositoryTest {

  @Test
  fun requests_the_station_publicly_by_uuid() = runTest {
    var request: HttpRequestData? = null
    repository(AIRBEAM_JSON) { request = it }.station("0b7e3f5a")

    assertEquals("/api/v3/explore/stations/0b7e3f5a", request!!.url.encodedPath)
    // `security: []`, like /explore/map: no token on a public endpoint.
    assertNull(request!!.headers[HttpHeaders.Authorization])
  }

  @Test
  fun maps_the_station_and_every_stream_to_domain() = runTest {
    val details = repository(AIRBEAM_JSON).station("0b7e3f5a")

    assertEquals(
      StationDetails(
        uuid = "0b7e3f5a", source = DataSource.AirBeam, title = "Home AirBeam Garden", provider = "jane",
        deviceModel = "AirBeamMini", timeZone = TimeZone.of("America/New_York"),
        streams = listOf(
          StationStream(35404, "AirBeam-PM1", "µg/m³", value = 8, min24h = 3, max24h = 10, measuredAt = MEASURED_AT),
          StationStream(35405, "AirBeam-PM2.5", "µg/m³", value = 12, min24h = 5, max24h = 20, measuredAt = MEASURED_AT),
        ),
      ),
      details,
    )
  }

  @Test
  fun a_station_without_device_model_maps_to_null() = runTest {
    val details = repository(AIRBEAM_JSON.replace(""""device_model":"AirBeamMini",""", "")).station("x")

    assertNull(details.deviceModel)
  }

  @Test
  fun a_quiet_station_still_opens_with_no_reading() = runTest {
    val json = AIRBEAM_JSON
      .replace("""{"value":8,"time":1793282400000}""", "null")
      .replace("""{"min":3,"max":10}""", "null")

    val pm1 = repository(json).station("x").streams.first()

    assertNull(pm1.value)
    assertNull(pm1.measuredAt)
    assertNull(pm1.min24h)
    assertNull(pm1.max24h)
  }

  private fun repository(json: String, onRequest: (HttpRequestData) -> Unit = {}): StationDetailsRepository {
    val engine = MockEngine { request ->
      onRequest(request)
      respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return NetworkStationDetailsRepository(ExploreMapApi(createAircastingHttpClient(engine)))
  }
}

private const val AIRBEAM_JSON = """
{"uuid":"0b7e3f5a","source":"airbeam","title":"Home AirBeam Garden","provider":"jane",
 "latitude":40.1,"longitude":-73.2,"active":true,"device_model":"AirBeamMini","time_zone":"America/New_York",
 "share_url":"https://aircasting.org/s/ab12c",
 "streams":[
  {"stream_id":35404,"sensor_name":"AirBeam-PM1","measurement_type":"Particulate Matter","unit_symbol":"µg/m³",
   "update_frequency":"1 minute","last_measurement":{"value":8,"time":1793282400000},"last_24h":{"min":3,"max":10}},
  {"stream_id":35405,"sensor_name":"AirBeam-PM2.5","measurement_type":"Particulate Matter","unit_symbol":"µg/m³",
   "update_frequency":"1 minute","last_measurement":{"value":12,"time":1793282400000},"last_24h":{"min":5,"max":20}}]}
"""

/** `last_measurement.time` in the JSON above: epoch ms, true UTC. */
private val MEASURED_AT = Instant.fromEpochMilliseconds(1793282400000)