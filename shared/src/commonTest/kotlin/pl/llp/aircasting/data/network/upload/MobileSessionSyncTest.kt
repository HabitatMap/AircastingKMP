package pl.llp.aircasting.data.network.upload

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementStream
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MobileSessionSyncTest {

  private val requests = mutableListOf<HttpRequestData>()
  private var createStatus = HttpStatusCode.Created
  private val outbox = FakeUploadOutbox()
  private val sessions = FakeSessionRepository(SESSION)
  private val streams = FakeStreamRepository(
    listOf(
      MeasurementStream(
        id = 1,
        sessionUuid = UUID,
        sensorName = "AirBeamMini-PM2.5",
        unitSymbol = "µg/m³"
      ),
      MeasurementStream(
        id = 2,
        sessionUuid = UUID,
        sensorName = "AirBeamMini-PM1",
        unitSymbol = "µg/m³"
      ),
    )
  )

  @Test
  fun creates_the_session_with_every_stream_and_writes_back_sensor_type_ids() = runTest {
    sync().sync(UUID)

    val body = Json.parseToJsonElement(requests.single { it.url.encodedPath == CREATE_PATH }.bodyText()).jsonObject
    assertNull(body["session"]) // flat body, no wrapper
    assertEquals(UUID, body["uuid"]?.jsonPrimitive?.content)
    assertEquals("Morning ride", body["title"]?.jsonPrimitive?.content)
    assertEquals("Europe/Warsaw", body["time_zone"]?.jsonPrimitive?.content)
    assertEquals(true, body["contribute"]?.jsonPrimitive?.boolean)
    assertEquals("bike commute", body["tag_list"]?.jsonPrimitive?.content)
    assertEquals("AA:BB:CC:DD:EE:FF", body["device"]?.jsonObject?.get("mac_address")?.jsonPrimitive?.content)
    assertEquals("AirBeamMini", body["device"]?.jsonObject?.get("model")?.jsonPrimitive?.content)
    assertEquals(
      listOf("AirBeamMini-PM2.5", "AirBeamMini-PM1"),
      body["streams"]?.jsonArray?.map { it.jsonObject["sensor_name"]?.jsonPrimitive?.content },
    )
    assertEquals(listOf(2, 1), streams.streams.map { it.sensorTypeId })
  }

  @Test
  fun pending_measurements_go_out_with_the_ids_the_create_just_assigned() = runTest {
    outbox.rows += Measurement(streamId = 1, time = START, value = 12.5, location = LOCATION)

    sync().sync(UUID)

    assertTrue(requests.any { it.url.encodedPath == "/api/v3/mobile_sessions/$UUID/measurements" })
    assertTrue(outbox.rows.isEmpty())
  }

  @Test
  fun session_already_on_the_server_is_not_created_again() = runTest {
    streams.setSensorTypeId(streamId = 1, sensorTypeId = 2)
    streams.setSensorTypeId(streamId = 2, sensorTypeId = 1)

    sync().sync(UUID)

    assertTrue(requests.none { it.url.encodedPath == CREATE_PATH })
  }

  @Test
  fun uuid_taken_reads_the_ids_back_from_the_existing_session() = runTest {
    createStatus = HttpStatusCode.Conflict

    sync().sync(UUID)

    assertEquals(listOf(2, 1), streams.streams.map { it.sensorTypeId })
  }

  @Test
  fun stopped_session_sends_its_end_time_to_finish_exactly_once() = runTest {
    sessions.session = SESSION.copy(endTime = END)
    val sync = sync()

    sync.sync(UUID)
    sync.sync(UUID)

    val finish = requests.single { it.url.encodedPath == FINISH_PATH }
    val body = Json.parseToJsonElement(finish.bodyText()).jsonObject
    assertEquals(END.toEpochMilliseconds(), body["finished_at"]?.jsonPrimitive?.long)
  }

  @Test
  fun session_still_recording_is_not_finished() = runTest {
    sync().sync(UUID)

    assertTrue(requests.none { it.url.encodedPath == FINISH_PATH })
  }
  @Test
  fun failed_create_throws_and_neither_uploads_nor_finishes() = runTest {
    createStatus = HttpStatusCode.InternalServerError
    sessions.session = SESSION.copy(endTime = END)
    outbox.rows += Measurement(streamId = 1, time = START, value = 12.5, location = LOCATION)

    assertFailsWith<ServerResponseException> { sync().sync(UUID) }

    assertEquals(listOf(CREATE_PATH), requests.map { it.url.encodedPath })
    assertEquals(1, outbox.rows.size)
  }

  @Test
  fun signed_out_sends_nothing() = runTest {
    sync(token = null).sync(UUID)

    assertTrue(requests.isEmpty())
  }
  private fun sync(token: String? = "tok-123"): MobileSessionSync {
    val engine = MockEngine { request ->
      requests += request
      when (request.url.encodedPath) {
        CREATE_PATH ->
          if (createStatus == HttpStatusCode.Created) respond(CREATED_JSON, createStatus, JSON_HEADERS)
          else respondError(createStatus)
        "/api/v3/mobile_sessions/$UUID" -> respond(SHOW_JSON, HttpStatusCode.OK, JSON_HEADERS)
        else -> respond("") // /measurements and /finish: only the status matters
      }
    }
    val api = MobileSessionsApi(createAircastingHttpClient(engine))
    val auth = StoredAuthSession(MapSettings()).apply { token?.let(::start) }
    return MobileSessionSync(
      api = api,
      sessions = sessions,
      streams = streams,
      uploader = MobileMeasurementUploader(api, outbox, streams, auth),
      auth = auth,
    )
  }
  private fun HttpRequestData.bodyText() = (body as TextContent).text

  private class FakeSessionRepository(var session: Session) : SessionRepository {
    override suspend fun find(uuid: String) = session.takeIf { it.uuid == uuid }
    override suspend fun markFinishedOnServer(uuid: String) { session = session.copy(isFinishedOnServer = true) }
    override suspend fun insert(session: Session) = error("unused")
    override fun observe(uuid: String): Flow<Session?> = error("unused")
    override fun observeAll(): Flow<List<Session>> = error("unused")
    override suspend fun updateStatus(uuid: String, status: SessionStatus) = error("unused")
    override suspend fun finish(uuid: String, endTime: Instant) = error("unused")
    override suspend fun delete(uuid: String) = error("unused")
  }
  private companion object {
    const val UUID = "8d7a3f2e-0000-4000-8000-000000000001"
    const val CREATE_PATH = "/api/v3/mobile_sessions"
    const val FINISH_PATH = "/api/v3/mobile_sessions/$UUID/finish"
    val START = Instant.parse("2026-10-19T08:00:00Z")
    val END = Instant.parse("2026-10-19T08:30:00Z")
    val LOCATION = GeoLocation(latitude = 52.23, longitude = 21.01)
    val SESSION = Session(
      uuid = UUID,
      type = SessionType.MOBILE,
      status = SessionStatus.RECORDING,
      name = "Morning ride",
      tags = listOf("bike", "commute"),
      startTime = START,
      timeZone = "Europe/Warsaw",
      measurementInterval = 1.seconds,
      deviceId = "AA:BB:CC:DD:EE:FF",
      deviceModel = "AirBeamMini",
    )
    val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    val CREATED_JSON = """
      {"share_url": "https://aircasting.org/s/ab12c",
       "streams": [{"sensor_name": "AirBeamMini-PM2.5", "sensor_type_id": 2},
                   {"sensor_name": "AirBeamMini-PM1", "sensor_type_id": 1}]}
    """.trimIndent()
    val SHOW_JSON = """
    {"uuid": "$UUID", "title": "Morning ride", "time_zone": "Europe/Warsaw",
       "streams": {"AirBeamMini-PM2.5": {"sensor_name": "AirBeamMini-PM2.5", "sensor_type_id": 2, "measurements_count": 0},
                   "AirBeamMini-PM1": {"sensor_name": "AirBeamMini-PM1", "sensor_type_id": 1, "measurements_count": 0}}}
    """.trimIndent()
  }
}