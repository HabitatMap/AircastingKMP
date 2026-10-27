package pl.llp.aircasting.recording

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.FixedSessionsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Backend half of a fixed start (DOMAIN_KNOWLEDGE.md "Starting a fixed session"): create on v3, then tags. */
class FixedSessionRegistrarTest {

  private val requests = mutableListOf<HttpRequestData>()
  private var createStatus = HttpStatusCode.Created
  private var patchStatus = HttpStatusCode.OK

  @Test
  fun outdoor_session_is_created_with_a_flat_body_at_its_location() = runTest {
    registrar().register(OUTDOOR)

    val body = bodyOf(CREATE_PATH)
    assertNull(body["session"]) // flat, no wrapper
    assertEquals(UUID.toString(), body.string("uuid"))
    assertEquals("Rooftop", body.string("title"))
    assertEquals(50.06, body["latitude"]?.jsonPrimitive?.double)
    assertEquals(19.94, body["longitude"]?.jsonPrimitive?.double)
    assertEquals(false, body["is_indoor"]?.jsonPrimitive?.boolean)
    assertEquals(true, body["contribute"]?.jsonPrimitive?.boolean)
    assertEquals("Europe/Warsaw", body.string("time_zone"))
    assertEquals("AA:BB:CC:DD:EE:FF", body["device"]?.jsonObject?.string("mac_address"))
    assertEquals("AirBeamMini", body["device"]?.jsonObject?.string("model"))
    assertEquals(
      listOf("AirBeamMini-PM1", "AirBeamMini-PM2.5"),
      body["streams"]?.jsonArray?.map { it.jsonObject.string("sensor_name") },
    )
  }

  @Test
  fun indoor_session_sends_the_200_200_placeholder() = runTest {
    registrar().register(OUTDOOR.copy(placement = Placement.Indoor))

    val body = bodyOf(CREATE_PATH)
    assertEquals(200.0, body["latitude"]?.jsonPrimitive?.double)
    assertEquals(200.0, body["longitude"]?.jsonPrimitive?.double)
    assertEquals(true, body["is_indoor"]?.jsonPrimitive?.boolean)
    assertEquals("Europe/Warsaw", body.string("time_zone"))
  }

  @Test
  fun returns_the_session_token_and_sensor_type_ids_by_sensor_name() = runTest {
    val registered = registrar().register(OUTDOOR)

    assertEquals(
      RegisteredFixedSession(
        sessionToken = TOKEN,
        sensorTypeIds = mapOf("AirBeam-PM1" to 1, "AirBeam-PM2.5" to 2),
      ),
      registered,
    )
  }

  @Test
  fun tags_are_set_by_a_patch_after_the_create() = runTest {
    registrar().register(OUTDOOR.copy(tags = listOf("home", "alaska")))

    assertEquals(listOf(CREATE_PATH, SESSION_PATH), requests.map { it.url.encodedPath })
    assertEquals(HttpMethod.Patch, requests.last().method)
    assertEquals("home alaska", bodyOf(SESSION_PATH).string("tag_list"))
  }

  @Test
  fun no_tags_no_patch() = runTest {
    registrar().register(OUTDOOR)

    assertEquals(listOf(CREATE_PATH), requests.map { it.url.encodedPath })
  }

  @Test
  fun a_failed_tag_patch_does_not_fail_the_registration() = runTest {
    patchStatus = HttpStatusCode.InternalServerError

    val registered = registrar().register(OUTDOOR.copy(tags = listOf("home")))

    assertEquals(TOKEN, registered?.sessionToken)
  }

  @Test
  fun failed_create_throws_and_sends_no_patch() = runTest {
    createStatus = HttpStatusCode.InternalServerError

    assertFailsWith<ServerResponseException> { registrar().register(OUTDOOR.copy(tags = listOf("home"))) }

    assertEquals(listOf(CREATE_PATH), requests.map { it.url.encodedPath })
  }

  @Test
  fun signed_out_registers_nothing() = runTest {
    assertNull(registrar(token = null).register(OUTDOOR))
    assertTrue(requests.isEmpty())
  }

  private fun registrar(token: String? = "tok-123"): FixedSessionRegistrar {
    val engine = MockEngine { request ->
      requests += request
      when (request.url.encodedPath) {
        CREATE_PATH ->
          if (createStatus == HttpStatusCode.Created) respond(CREATED_JSON, createStatus, JSON_HEADERS)
          else respondError(createStatus)
        else ->
          if (patchStatus == HttpStatusCode.OK) respond("{}", patchStatus, JSON_HEADERS)
          else respondError(patchStatus)
      }
    }
    val auth = StoredAuthSession(MapSettings()).apply { token?.let { start(it, AccountProfile("ada", "ada@example.com")) } }
    return FixedSessionRegistrar(FixedSessionsApi(createAircastingHttpClient(engine)), auth, TimeZone.of("Europe/Warsaw"))
  }

  private fun bodyOf(path: String): JsonObject =
    Json.parseToJsonElement((requests.single { it.url.encodedPath == path }.body as TextContent).text).jsonObject

  private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.content

  private companion object {
    val UUID = Uuid.parse("8d7a3f2e-0000-4000-8000-000000000002")
    const val CREATE_PATH = "/api/v3/fixed_sessions"
    val SESSION_PATH = "/api/v3/fixed_sessions/$UUID"
    const val TOKEN = "0123456789abcdef0123456789abcdef"
    val OUTDOOR = NewFixedSession(
      uuid = UUID,
      name = "Rooftop",
      tags = emptyList(),
      device = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2),
      placement = Placement.Outdoor(GeoLocation(latitude = 50.06, longitude = 19.94)),
    )
    val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    val CREATED_JSON = """
      {"share_url": "https://aircasting.org/s/ab12c", "location": "https://aircasting.org/s/ab12c",
       "session_token": "$TOKEN",
       "streams": [{"sensor_name": "AirBeam-PM1", "sensor_type_id": 1},
                   {"sensor_name": "AirBeam-PM2.5", "sensor_type_id": 2}]}
    """.trimIndent()
  }
}