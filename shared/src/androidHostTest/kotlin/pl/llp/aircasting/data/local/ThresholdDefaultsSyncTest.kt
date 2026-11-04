package pl.llp.aircasting.data.local

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.data.network.ThresholdSetsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.ThresholdDefaultsSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class ThresholdDefaultsSyncTest {

  private val queries = testDatabase().sensorThresholdQueries
  private val repository = LocalThresholdsRepository(queries)

  private var request: HttpRequestData? = null
  private var status = HttpStatusCode.OK
  private val sync = ThresholdDefaultsSync(
    ThresholdSetsApi(
      createAircastingHttpClient(
        MockEngine { req ->
          request = req
          respond(THRESHOLD_SETS_JSON, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
      ),
    ),
    queries,
  )

  private val seededPm25 = SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)
  private val serverPm25 = SensorThreshold("AirBeam-PM2.5", 0, 12, 35, 55, 150)

  private suspend fun forSensor(name: String) = repository.observe().first().forSensor(name)

  @Test
  fun requestsThresholdSetsPublicly() = runTest {
    sync.refresh()

    assertEquals("/api/v3/threshold_sets", request!!.url.encodedPath)
    assertNull(request!!.headers[HttpHeaders.Authorization])
  }

  @Test
  fun serverValuesReplaceTheSeed() = runTest {
    sync.refresh()

    assertEquals(serverPm25, forSensor("AirBeam3-PM2.5"))
  }

  @Test
  fun aNewServerAliasResolvesToItsParameter() = runTest {
    sync.refresh()

    assertEquals(serverPm25, forSensor("AirBeam4-PM2.5"))
  }

  @Test
  fun whatTheServerNoLongerListsIsDropped() = runTest {
    sync.refresh()

    assertNull(forSensor("Government-PM2.5")) // alias dropped
    assertNull(forSensor("AirBeam-PM10")) // whole parameter dropped
  }


  @Test
  fun userOverrideSurvivesRefresh() = runTest {
    repository.save(seededPm25.copy(low = 20))

    sync.refresh()

    assertEquals(20, forSensor("AirBeamMini-PM2.5")?.low)
  }

  @Test
  fun failedRefreshKeepsStoredDefaults() = runTest {
    status = HttpStatusCode.InternalServerError

    assertFails { sync.refresh() }

    assertEquals(seededPm25, forSensor("AirBeam3-PM2.5"))
  }
}

/** Trimmed live shape: floats, extra fields the client ignores, PM10 and `Government-PM2.5` gone, `AirBeam4-PM2.5` new. */
private const val THRESHOLD_SETS_JSON = """
{"threshold_sets":[
  {"sensor_name":"AirBeam-PM2.5","measurement_type":"Particulate Matter","unit_symbol":"µg/m³",
   "aliases":["AirBeam-PM","AirBeam-PM2.5","AirBeam2-PM2.5","AirBeam3-PM2.5","AirBeamMini-PM2.5","AirBeam4-PM2.5"],
   "thresholds":{"very_low":0.0,"low":12.0,"medium":35.0,"high":55.0,"very_high":150.0}},
  {"sensor_name":"Phone Microphone","measurement_type":"Sound Level","unit_symbol":"dB",
   "aliases":["Phone Microphone"],
   "thresholds":{"very_low":20.0,"low":60.0,"medium":70.0,"high":80.0,"very_high":100.0}}
]}
"""