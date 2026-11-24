package pl.llp.aircasting.data.network

import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.active.ActiveFixedSession
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.StoredAuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NetworkActiveFixedSessionsRepositoryTest {
  private val requests = mutableListOf<HttpRequestData>()
  private val auth = StoredAuthSession(MapSettings())
  private var failing = false

  @Test
  fun lists_own_sessions_with_the_user_token_and_maps_them() = runTest {
    auth.start("user-token", PROFILE)

    repository().observeFixed().test {
      assertEquals(listOf(LIVING_ROOM), awaitItem())
    }
    assertEquals("/api/v3/fixed_sessions", requests.single().url.encodedPath)
    assertEquals("Bearer user-token", requests.single().headers[HttpHeaders.Authorization])
  }

  @Test
  fun finished_sessions_are_not_active() = runTest {
    auth.start("user-token", PROFILE)

    repository(json = LIST_JSON.replace(""""finished_at":null""", """"finished_at":1786707060000"""))
      .observeFixed().test { assertEquals(emptyList(), awaitItem()) }
  }

  @Test
  fun signed_out_is_empty_without_asking_the_server() = runTest {
    repository().observeFixed().test { assertEquals(emptyList(), awaitItem()) }
    assertEquals(emptyList(), requests)
  }

  @Test
  fun polls_again_after_the_interval() = runTest {
    auth.start("user-token", PROFILE)

    repository().observeFixed().test {
      awaitItem()
      advanceTimeBy(POLL + 1.seconds)
      awaitItem()
    }
    assertEquals(2, requests.size)
  }

  @Test
  fun refresh_fetches_without_waiting_for_the_poll() = runTest {
    auth.start("user-token", PROFILE)
    val repository = repository()

    repository.observeFixed().test {
      awaitItem()
      repository.refresh()
      awaitItem()
    }
    assertEquals(2, requests.size)
  }

  @Test
  fun asks_for_one_page_of_the_newest_sessions() = runTest {
    auth.start("user-token", PROFILE)

    repository().observeFixed().test { awaitItem() }

    assertEquals("1", requests.single().url.parameters["page"])
    assertEquals("50", requests.single().url.parameters["per_page"])
  }

  @Test
  fun finish_drops_the_card_on_the_servers_answer_without_refetching() = runTest {
    auth.start("user-token", PROFILE)
    val repository = repository()

    repository.observeFixed().test {
      awaitItem()
      repository.finish("f1")
      assertEquals(emptyList(), awaitItem())
    }
    val finish = requests.single { it.url.encodedPath == "/api/v3/fixed_sessions/f1/finish" }
    assertEquals(HttpMethod.Post, finish.method)
    assertEquals("Bearer user-token", finish.headers[HttpHeaders.Authorization])
    assertEquals(1, requests.count { it.url.encodedPath == "/api/v3/fixed_sessions" })
  }

  @Test
  fun a_failed_finish_keeps_the_card() = runTest {
    auth.start("user-token", PROFILE)
    val repository = repository()

    repository.observeFixed().test {
      awaitItem()
      failing = true
      repository.finish("f1")
      expectNoEvents()
    }
  }

  @Test
  fun a_failed_fetch_emits_nothing_and_the_next_one_recovers() = runTest {
    auth.start("user-token", PROFILE)
    failing = true
    val repository = repository()

    repository.observeFixed().test {
      expectNoEvents()
      failing = false
      repository.refresh()
      assertEquals(listOf(LIVING_ROOM), awaitItem())
    }
  }

  /** The engine runs on the test scheduler: otherwise virtual time races ahead of real I/O and polls early. */
  private fun TestScope.repository(json: String = LIST_JSON): NetworkActiveFixedSessionsRepository {
    val engine = MockEngine(
      MockEngineConfig().apply {
        dispatcher = UnconfinedTestDispatcher(testScheduler)
        addHandler { request ->
          requests += request
          val body = if (request.url.encodedPath.endsWith("/finish")) FINISH_JSON else json
          if (failing) respondError(HttpStatusCode.InternalServerError)
          else respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
      },
    )
    return NetworkActiveFixedSessionsRepository(FixedSessionsApi(createAircastingHttpClient(engine)), auth, pollEvery = POLL)
  }

  private companion object {
    val POLL = 60.seconds
    val PROFILE = AccountProfile("u", "u@example.com")
    val LIVING_ROOM = ActiveFixedSession(
      uuid = "f1",
      name = "Living room",
      tags = listOf("Home", "Alaska"),
      deviceModel = "AirBeam3",
      isIndoor = true,
      timeZone = "America/New_York",
      startTime = Instant.fromEpochMilliseconds(1786663800000),
      lastReportedAt = Instant.fromEpochMilliseconds(1786707000000),
      readings = listOf(
        LatestReading("AirBeam3-PM2.5", "µg/m³", 12.5, Instant.fromEpochMilliseconds(1786706940000)),
        LatestReading("AirBeam3-PM1", "µg/m³"),
      ),
    )
    const val FINISH_JSON = """{"uuid":"f1","finished_at":1786707060000,"version":2}"""
    const val LIST_JSON = """
{"meta":{"total":1,"page":1,"per_page":500,"total_pages":1},
 "sessions":[{"uuid":"f1","title":"Living room","type":"FixedSession","tag_list":"Home, Alaska",
  "contribute":true,"is_indoor":true,"time_zone":"America/New_York","start_time":1786663800000,
  "end_time":1786707000000,"last_measurement_at":1786707000000,"finished_at":null,"version":1,
  "latitude":200,"longitude":200,"share_url":"https://aircasting.org/s/ab12c",
  "device":{"mac_address":"aa:bb","model":"AirBeam3","name":null},
  "streams":{
   "AirBeam3-PM2.5":{"sensor_name":"AirBeam3-PM2.5","sensor_type_id":2,"unit_symbol":"µg/m³",
    "last_measurement":{"value":12.5,"time":1786706940000},"threshold_low":9},
   "AirBeam3-PM1":{"sensor_name":"AirBeam3-PM1","sensor_type_id":null,"unit_symbol":"µg/m³",
    "last_measurement":null}}}]}
"""
  }
}
