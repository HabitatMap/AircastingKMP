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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.active.ActiveFixedSession
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.local.FixedSessionDetailsRepository
import pl.llp.aircasting.data.local.LocalActiveFixedSessionsRepository
import pl.llp.aircasting.data.local.LocalPastSessionsRepository
import pl.llp.aircasting.data.local.LocalSessionDetailsRepository
import pl.llp.aircasting.data.local.SyncingPastSessionsRepository
import pl.llp.aircasting.data.local.testDatabase
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The user's fixed sessions: synced from /api/v3/fixed_sessions into the DB, the Active tab reads the DB. */
class FixedSessionsSyncTest {
  private val db = testDatabase()
  private val requests = mutableListOf<HttpRequestData>()
  private val auth = StoredAuthSession(MapSettings())
  private var failing = false
  private var listJson = LIST_JSON
  private var listPages = emptyMap<Int, String>()
  private var sessionJson = SESSION_JSON
  private var measurementsJson = "[]"

  @Test
  fun lists_one_page_of_own_sessions_with_the_user_token() = runTest {
    auth.start("user-token", PROFILE)

    sync().syncList()

    val request = requests.single()
    assertEquals("/api/v3/fixed_sessions", request.url.encodedPath)
    assertEquals("Bearer user-token", request.headers[HttpHeaders.Authorization])
    assertEquals("1", request.url.parameters["page"])
    assertEquals("50", request.url.parameters["per_page"])
  }

  @Test
  fun a_synced_session_shows_on_the_active_tab_with_its_newest_readings() = runTest {
    auth.start("user-token", PROFILE)

    repository().observeFixed().test {
      assertEquals(emptyList(), awaitItem()) // the empty DB, before the sync answers
      assertEquals(listOf(LIVING_ROOM), awaitItem())
    }
  }

  @Test
  fun stored_sessions_show_at_once_even_when_the_sync_fails() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    failing = true

    repository().observeFixed().test {
      assertEquals(listOf(LIVING_ROOM), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun a_session_finished_on_the_server_is_stored_as_finished_and_not_active() = runTest {
    auth.start("user-token", PROFILE)
    listJson = LIST_JSON.replace(""""finished_at":null""", """"finished_at":$FINISHED_AT""")

    sync().syncList()

    val row = db.sessionQueries.selectSessionByUuid("f1").executeAsOne()
    assertEquals(SessionStatus.FINISHED, row.status)
    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), row.end_time)
    assertEquals(emptyList(), db.fixedSessionQueries.selectActiveFixed().executeAsList())
  }

  @Test
  fun a_session_finished_elsewhere_gets_the_backends_finished_at_and_leaves_the_active_tab() = runTest {
    auth.start("user-token", PROFILE)
    val repository = repository()

    repository.observeFixed().test {
      skipItems(1) // the empty DB, before the first sync answers
      assertEquals(listOf(LIVING_ROOM), awaitItem())

      listJson = LIST_JSON.replace(""""finished_at":null""", """"finished_at":$FINISHED_AT""")
      repository.refresh()

      assertEquals(emptyList(), awaitItem())
    }
    val row = db.sessionQueries.selectSessionByUuid("f1").executeAsOne()
    assertEquals(SessionStatus.FINISHED, row.status)
    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), row.end_time)
  }

  @Test
  fun opening_past_sessions_syncs_and_shows_a_session_finished_on_the_server() = runTest {
    auth.start("user-token", PROFILE)
    listJson = LIST_JSON.replace(""""finished_at":null""", """"finished_at":$FINISHED_AT""")

    pastSessions().observe().test {
      assertEquals(emptyList(), awaitItem()) // the empty DB, before the sync answers
      assertEquals(listOf("f1"), awaitItem().map { it.session.uuid })
    }
    assertEquals(1, requests.size)
  }

  @Test
  fun past_sessions_walk_every_page_and_store_only_the_finished_ones() = runTest {
    auth.start("user-token", PROFILE)
    listPages = mapOf(
      1 to page(1, 2, session("f1"), session("live", finished = false)),
      2 to page(2, 2, session("f2")),
    )

    sync().syncFinished()

    assertEquals(listOf("1", "2"), requests.map { it.url.parameters["page"] })
    assertEquals(listOf("50", "50"), requests.map { it.url.parameters["per_page"] })
    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), db.sessionQueries.selectSessionByUuid("f2").executeAsOne().end_time)
    assertEquals(null, db.sessionQueries.selectSessionByUuid("live").executeAsOneOrNull())
  }

  @Test
  fun opening_details_of_a_session_finished_elsewhere_finishes_it() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    sessionJson = SESSION_JSON.replace(""""finished_at":null""", """"finished_at":$FINISHED_AT""")

    sync().syncSession("f1")

    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), db.sessionQueries.selectSessionByUuid("f1").executeAsOne().end_time)
  }

  @Test
  fun a_stored_session_missing_from_the_answer_stays_active() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    listJson = EMPTY_LIST_JSON

    sync().syncList()

    assertEquals(listOf(LIVING_ROOM), repository().observeFixed().first())
  }

  @Test
  fun a_list_fetched_before_the_finish_answer_does_not_reopen_the_session() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    sync().finish("f1")

    sync().syncList() // still says finished_at null, e.g. answered just before the finish

    val row = db.sessionQueries.selectSessionByUuid("f1").executeAsOne()
    assertEquals(SessionStatus.FINISHED, row.status)
    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), row.end_time)
  }

  @Test
  fun signed_out_asks_the_server_nothing() = runTest {
    sync().syncList()

    assertEquals(emptyList(), requests)
  }

  @Test
  fun polls_again_after_the_interval() = runTest {
    auth.start("user-token", PROFILE)

    repository().observeFixed().test {
      advanceTimeBy(POLL + 1.seconds)
      cancelAndIgnoreRemainingEvents()
    }
    assertEquals(2, requests.size)
  }

  @Test
  fun refresh_syncs_without_waiting_for_the_poll() = runTest {
    auth.start("user-token", PROFILE)
    val repository = repository()

    repository.observeFixed().test {
      repository.refresh()
      cancelAndIgnoreRemainingEvents()
    }
    assertEquals(2, requests.size)
  }

  @Test
  fun finish_marks_the_session_finished_from_the_servers_answer() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()

    sync().finish("f1")

    val finish = requests.single { it.url.encodedPath == "/api/v3/fixed_sessions/f1/finish" }
    assertEquals(HttpMethod.Post, finish.method)
    assertEquals("Bearer user-token", finish.headers[HttpHeaders.Authorization])
    assertEquals(Instant.fromEpochMilliseconds(FINISHED_AT), db.sessionQueries.selectSessionByUuid("f1").executeAsOne().end_time)
  }

  @Test
  fun a_failed_finish_keeps_the_session_active() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    failing = true

    sync().finish("f1")

    assertEquals(listOf(LIVING_ROOM), repository().observeFixed().first())
  }

  @Test
  fun details_show_the_stored_session_then_the_synced_one() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    sessionJson = SESSION_JSON.replace("Living room", "Bedroom")

    details().observe("f1").test {
      assertEquals("Living room", awaitItem()?.title)
      assertEquals("Bedroom", awaitItem()?.title)
    }
    assertEquals(1, requests.count { it.url.encodedPath == "/api/v3/fixed_sessions/f1" })
  }

  @Test
  fun a_created_session_with_no_measurements_yet_is_stored_without_readings() = runTest {
    auth.start("user-token", PROFILE)
    listJson = LIST_JSON
      .replace(""""last_measurement_at":1786707000000""", """"last_measurement_at":null""")
      .replace(""""last_measurement":{"value":12.5,"time":1786706940000}""", """"last_measurement":null""")

    sync().syncList()

    val session = repository().observeFixed().first().single()
    assertEquals(null, session.lastReportedAt)
    assertEquals(listOf(null, null), session.readings.map { it.value }) // the card's "No data yet"
  }

  @Test
  fun a_session_without_a_start_is_not_stored() = runTest {
    auth.start("user-token", PROFILE)
    listJson = LIST_JSON.replace(""""start_time":1786663800000""", """"start_time":null""")

    sync().syncList()

    assertEquals(null, db.sessionQueries.selectSessionByUuid("f1").executeAsOneOrNull())
  }

  @Test
  fun the_graph_fetches_the_24h_before_the_newest_reading_in_12h_windows() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    requests.clear()

    sync().syncMeasurements("f1", PM25)

    assertEquals(2, requests.size)
    assertEquals(setOf("/api/v3/fixed_sessions/f1/measurements"), requests.map { it.url.encodedPath }.toSet())
    assertEquals((NEWEST - 24.hours).toEpochMilliseconds(), requests.minOf { it.startTime() })
    assertEquals(NEWEST.toEpochMilliseconds(), requests.maxOf { it.endTime() })
  }

  @Test
  fun a_reopened_graph_fetches_only_after_the_previous_graph_sync() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    sync().syncMeasurements("f1", PM25)
    listJson = LIST_JSON.replace(""""time":1786706940000""", """"time":${(NEWEST + 5.minutes).toEpochMilliseconds()}""")
    sync().syncList() // stores the new newest reading alone, past a 5 min gap
    requests.clear()

    sync().syncMeasurements("f1", PM25)

    val request = requests.single()
    assertEquals((NEWEST + 1.milliseconds).toEpochMilliseconds(), request.startTime())
    assertEquals((NEWEST + 5.minutes).toEpochMilliseconds(), request.endTime())
  }

  @Test
  fun nothing_newer_than_the_previous_graph_sync_asks_the_server_nothing() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    sync().syncMeasurements("f1", PM25)
    requests.clear()

    sync().syncMeasurements("f1", PM25)

    assertEquals(emptyList(), requests)
  }

  @Test
  fun a_stream_that_never_reported_asks_the_server_nothing() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    requests.clear()

    sync().syncMeasurements("f1", "AirBeam3-PM1")

    assertEquals(emptyList(), requests)
  }

  @Test
  fun the_graph_shows_the_fetched_readings_and_drops_those_older_than_24h() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    measurementsJson = readings(NEWEST - 25.hours, NEWEST - 2.hours, NEWEST - 1.hours)

    details().observePoints("f1", PM25, TimeZone.UTC).test {
      skipItems(1) // the newest reading alone, before the sync answers
      // The server's 25 h old reading lies outside the 24 h: dropped, so 2 fetched + the newest.
      assertEquals(3, awaitItem().size)
    }
  }

  @Test
  fun signing_out_deletes_the_fixed_sessions_with_their_readings_but_not_mobile_ones() = runTest {
    auth.start("user-token", PROFILE)
    sync().syncList()
    db.sessionQueries.insertSession(
      uuid = "m1", type = SessionType.MOBILE, status = SessionStatus.FINISHED, name = "Commute",
      tags = emptyList(), device_id = null, device_model = null, start_time = NEWEST, end_time = NEWEST,
      latitude = null, longitude = null, is_indoor = false, is_contributing_to_crowd_map = true,
      time_zone = "UTC", measurement_interval_millis = 1_000, session_token = null,
    )
    val sync = sync()
    backgroundScope.launch { sync.clearWhenSignedOut() } // never returns; cancelled with the test
    runCurrent()
    assertEquals(1, db.fixedSessionQueries.selectActiveFixed().executeAsList().size) // signed in: kept

    auth.end()
    runCurrent()

    assertEquals(null, db.sessionQueries.selectSessionByUuid("f1").executeAsOneOrNull())
    assertEquals(null, db.measurementStreamQueries.selectStream("f1", PM25).executeAsOneOrNull())
    assertEquals("Commute", db.sessionQueries.selectSessionByUuid("m1").executeAsOne().name)
  }

  private fun HttpRequestData.startTime() = url.parameters["start_time"]!!.toLong()
  private fun HttpRequestData.endTime() = url.parameters["end_time"]!!.toLong()

  private fun readings(vararg times: Instant) =
    times.joinToString(",", "[", "]") { """{"time":${it.toEpochMilliseconds()},"value":5.0}""" }

  private fun page(page: Int, totalPages: Int, vararg sessions: String) =
    """{"meta":{"total":${totalPages * 50},"page":$page,"per_page":50,"total_pages":$totalPages},""" +
      """"sessions":[${sessions.joinToString(",")}]}"""

  private fun session(uuid: String, finished: Boolean = true) =
    SESSION_JSON.replace(""""f1"""", """"$uuid"""")
      .let { if (finished) it.replace(""""finished_at":null""", """"finished_at":$FINISHED_AT""") else it }

  /** The engine and DB reads run on the test scheduler: otherwise virtual time races ahead and polls early. */
  private fun TestScope.sync(): FixedSessionsSync {
    val engine = MockEngine(
      MockEngineConfig().apply {
        dispatcher = UnconfinedTestDispatcher(testScheduler)
        addHandler { request ->
          requests += request
          val body = when (request.url.encodedPath) {
            "/api/v3/fixed_sessions/f1/finish" -> FINISH_JSON
            "/api/v3/fixed_sessions/f1" -> sessionJson
            "/api/v3/fixed_sessions/f1/measurements" -> measurementsJson
            else -> listPages[request.url.parameters["page"]?.toInt()] ?: listJson
          }
          if (failing) respondError(HttpStatusCode.InternalServerError)
          else respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
      },
    )
    return FixedSessionsSync(
      FixedSessionsApi(createAircastingHttpClient(engine)), auth, db, UnconfinedTestDispatcher(testScheduler),
    )
  }

  private fun TestScope.repository() =
    LocalActiveFixedSessionsRepository(db.fixedSessionQueries, sync(), POLL, UnconfinedTestDispatcher(testScheduler))

  private fun TestScope.details() = FixedSessionDetailsRepository(
    LocalSessionDetailsRepository(db.sessionQueries, UnconfinedTestDispatcher(testScheduler)), sync(),
  )

  private fun TestScope.pastSessions() = SyncingPastSessionsRepository(
    LocalPastSessionsRepository(db.sessionQueries, UnconfinedTestDispatcher(testScheduler)), sync(),
  )

  private companion object {
    val POLL = 60.seconds
    const val PM25 = "AirBeam3-PM2.5"
    /** The PM2.5 stream's newest reading in the list answer. */
    val NEWEST: Instant = Instant.fromEpochMilliseconds(1786706940000)
    val PROFILE = AccountProfile("u", "u@example.com")
    const val FINISHED_AT = 1786707060000
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
    const val FINISH_JSON = """{"uuid":"f1","finished_at":$FINISHED_AT,"version":2}"""
    const val EMPTY_LIST_JSON = """{"meta":{"total":0,"page":1,"per_page":50,"total_pages":0},"sessions":[]}"""
    const val SESSION_JSON = """
{"uuid":"f1","title":"Living room","type":"FixedSession","tag_list":"Home, Alaska",
  "contribute":true,"is_indoor":true,"time_zone":"America/New_York","start_time":1786663800000,
  "end_time":1786707000000,"last_measurement_at":1786707000000,"finished_at":null,"version":1,
  "latitude":200,"longitude":200,"share_url":"https://aircasting.org/s/ab12c",
  "device":{"mac_address":"aa:bb","model":"AirBeam3","name":null},
  "streams":{
   "AirBeam3-PM2.5":{"sensor_name":"AirBeam3-PM2.5","sensor_type_id":2,"unit_symbol":"µg/m³",
    "last_measurement":{"value":12.5,"time":1786706940000},"threshold_low":9},
   "AirBeam3-PM1":{"sensor_name":"AirBeam3-PM1","sensor_type_id":null,"unit_symbol":"µg/m³",
    "last_measurement":null}}}
"""
    const val LIST_JSON = """{"meta":{"total":1,"page":1,"per_page":500,"total_pages":1},"sessions":[$SESSION_JSON]}"""
  }
}
