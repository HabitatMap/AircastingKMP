package pl.llp.aircasting.data.network.upload

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MobileMeasurementUploaderTest {

  private val requests = mutableListOf<HttpRequestData>()
  private val outbox = FakeUploadOutbox()
  private val pm25 = MeasurementStream(id = 1, sessionUuid = UUID, sensorName = "AirBeam3-PM2.5", unitSymbol = "µg/m³", sensorTypeId = 3)
  private val pm1 = MeasurementStream(id = 2, sessionUuid = UUID, sensorName = "AirBeam3-PM1", unitSymbol = "µg/m³", sensorTypeId = 1)

  @Test
  fun sends_pending_measurements_as_one_binary_batch_then_acknowledges_them() = runTest {
    val a = measurement(streamId = 1, second = 0, value = 12.5)
    val b = measurement(streamId = 2, second = 0, value = 7.0)
    outbox.rows += listOf(a, b)

    uploader(streams = listOf(pm25, pm1)).upload(UUID)

    val request = requests.single()
    assertEquals("/api/v3/mobile_sessions/$UUID/measurements", request.url.encodedPath)
    assertEquals("Bearer tok-123", request.headers[HttpHeaders.Authorization])
    val expected = MobileMeasurementPayload.encode(
      listOf(
        MobileFrame(a.time, sensorTypeId = 3, value = 12.5, location = LOCATION),
        MobileFrame(b.time, sensorTypeId = 1, value = 7.0, location = LOCATION),
      )
    )
    assertContentEquals(expected, request.bodyBytes())
    assertTrue(outbox.rows.isEmpty())
  }
  @Test
  fun empty_outbox_sends_nothing() = runTest {
    uploader(streams = listOf(pm25)).upload(UUID)

    assertTrue(requests.isEmpty())
  }

  @Test
  fun failed_request_throws_and_keeps_the_rows_queued() = runTest {
    outbox.rows += measurement(streamId = 1, second = 0)

    assertFailsWith<ServerResponseException> {
      uploader(streams = listOf(pm25), status = HttpStatusCode.InternalServerError).upload(UUID)
    }
    assertEquals(1, outbox.rows.size)
  }

  @Test
  fun full_batch_triggers_another_pass_until_a_short_batch_drains_the_outbox() = runTest {
    outbox.rows += List(MobileMeasurementPayload.MaxFrames + 1) { measurement(streamId = 1, second = it) }

    uploader(streams = listOf(pm25)).upload(UUID)

    assertEquals(2, requests.size)
    assertTrue(outbox.rows.isEmpty())
  }

  @Test
  fun rows_of_a_stream_without_sensor_type_id_stay_queued_and_the_rest_still_go() = runTest {
    val orphanStream = pm1.copy(sensorTypeId = null)
    val sendable = measurement(streamId = 1, second = 0)
    val orphan = measurement(streamId = 2, second = 0)
    outbox.rows += listOf(sendable, orphan)

    uploader(streams = listOf(pm25, orphanStream)).upload(UUID)

    assertEquals(1, requests.size)
    assertEquals(listOf(orphan), outbox.rows)
  }

  @Test
  fun signed_out_sends_nothing_and_keeps_the_rows_queued() = runTest {
    outbox.rows += measurement(streamId = 1, second = 0)

    uploader(streams = listOf(pm25), token = null).upload(UUID)

    assertTrue(requests.isEmpty())
    assertEquals(1, outbox.rows.size)
  }

  private fun uploader(
    streams: List<MeasurementStream>,
    status: HttpStatusCode = HttpStatusCode.OK,
    token: String? = "tok-123",
  ): MobileMeasurementUploader {
    val engine = MockEngine { request ->
      requests += request
      if (status == HttpStatusCode.OK) respond("") else respondError(status)
    }
    return MobileMeasurementUploader(
      api = MobileSessionsApi(createAircastingHttpClient(engine)),
      outbox = outbox,
      streams = FakeStreamRepository(streams),
      auth = StoredAuthSession(MapSettings()).apply { token?.let(::start) },
    )
  }

  private fun measurement(streamId: Long, second: Int, value: Double = 10.0) =
    Measurement(streamId = streamId, time = T0 + second.seconds, value = value, location = LOCATION)

  private fun HttpRequestData.bodyBytes() = (body as OutgoingContent.ByteArrayContent).bytes()

  private companion object {
    const val UUID = "8d7a3f2e-0000-4000-8000-000000000001"
    val T0 = Instant.parse("2024-01-01T00:00:00Z")
    val LOCATION = GeoLocation(latitude = 40.0, longitude = -74.0)
  }
}
