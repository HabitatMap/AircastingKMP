package pl.llp.aircasting.data.network.upload

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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.llp.aircasting.data.auth.AccountProfile
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class MobileNoteUploaderTest {

  private val requests = mutableListOf<HttpRequestData>()
  private val outbox = FakeNoteOutbox()

  @Test
  fun `posts a pending note and records the server id`() = runTest {
    outbox.notes += NOTE

    uploader().upload(UUID, WARSAW)

    val request = requests.single()
    assertEquals("/api/v3/mobile_sessions/$UUID/notes", request.url.encodedPath)
    assertEquals("Bearer tok-123", request.headers[HttpHeaders.Authorization])
    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
    assertEquals("Smells like smoke", body.getValue("text").jsonPrimitive.content)
    assertEquals(52.23, body.getValue("latitude").jsonPrimitive.content.toDouble())
    assertEquals(21.01, body.getValue("longitude").jsonPrimitive.content.toDouble())
    assertEquals(mapOf(NOTE.id to 17L), outbox.uploaded)
  }

  /**
   * The spec's `date` is the wall clock the phone saw, no zone: the session's zone, like the rest of
   * the session (10:00 in Warsaw is 08:00 UTC on CEST). Whole seconds, even when they are zero.
   */
  @Test
  fun `date is the session wall clock with whole seconds`() = runTest {
    outbox.notes += NOTE.copy(time = Instant.parse("2026-10-19T08:00:00.250Z"))

    uploader().upload(UUID, WARSAW)

    val body = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject
    assertEquals("2026-10-19T10:00:00", body.getValue("date").jsonPrimitive.content)
  }

  @Test
  fun `a note with a photo sends it base64 encoded`() = runTest {
    outbox.notes += NOTE.copy(hasPhoto = true)
    outbox.photos[NOTE.id] = "hello".encodeToByteArray()

    uploader().upload(UUID, WARSAW)

    val body = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject
    assertEquals("aGVsbG8=", body.getValue("photo").jsonPrimitive.content)
  }

  @Test
  fun `already uploaded notes are not sent again`() = runTest {
    outbox.notes += NOTE
    outbox.uploaded[NOTE.id] = 17

    uploader().upload(UUID, WARSAW)

    assertTrue(requests.isEmpty())
  }

  @Test
  fun `failed request throws and keeps the note pending`() = runTest {
    outbox.notes += NOTE

    assertFailsWith<ServerResponseException> { uploader(status = HttpStatusCode.InternalServerError).upload(UUID, WARSAW) }

    assertEquals(listOf(NOTE), outbox.pending(UUID))
  }

  @Test
  fun `an edited note is patched with its new text`() = runTest {
    outbox.edits += NoteChange(id = 1, remoteId = 17, text = "Empty street")

    uploader().upload(UUID, WARSAW)

    val request = requests.single()
    assertEquals(HttpMethod.Patch, request.method)
    assertEquals("/api/v3/mobile_sessions/$UUID/notes/17", request.url.encodedPath)
    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
    assertEquals("Empty street", body.getValue("text").jsonPrimitive.content)
    assertEquals(listOf("edited 1"), outbox.log)
  }

  @Test
  fun `a deleted note is deleted on the server then purged`() = runTest {
    outbox.deletes += NoteChange(id = 1, remoteId = 17, text = "Smells like smoke")

    uploader().upload(UUID, WARSAW)

    val request = requests.single()
    assertEquals(HttpMethod.Delete, request.method)
    assertEquals("/api/v3/mobile_sessions/$UUID/notes/17", request.url.encodedPath)
    assertEquals(listOf("purged 1"), outbox.log)
  }

  /** Deleted on the web meanwhile: the server already has what we want. */
  @Test
  fun `a delete the server answers with not found still purges`() = runTest {
    outbox.deletes += NoteChange(id = 1, remoteId = 17, text = "Smells like smoke")

    uploader(status = HttpStatusCode.NotFound).upload(UUID, WARSAW)

    assertEquals(listOf("purged 1"), outbox.log)
  }

  /** Deleted on the web meanwhile: nothing left to edit, so the edit is dropped rather than retried forever. */
  @Test
  fun `an edit the server answers with not found is dropped`() = runTest {
    outbox.edits += NoteChange(id = 1, remoteId = 17, text = "Empty street")

    uploader(status = HttpStatusCode.NotFound).upload(UUID, WARSAW)

    assertEquals(listOf("edited 1"), outbox.log)
  }

  @Test
  fun `a note deleted before it was ever sent is purged without a request`() = runTest {
    outbox.deletes += NoteChange(id = 1, remoteId = null, text = "Smells like smoke")

    uploader().upload(UUID, WARSAW)

    assertTrue(requests.isEmpty())
    assertEquals(listOf("purged 1"), outbox.log)
  }

  /** Creates first: a delete made while its create was in flight only has a server id afterwards. */
  @Test
  fun `creates go before edits and edits before deletes`() = runTest {
    outbox.notes += NOTE
    outbox.edits += NoteChange(id = 2, remoteId = 18, text = "Empty street")
    outbox.deletes += NoteChange(id = 3, remoteId = 19, text = "Gone")

    uploader().upload(UUID, WARSAW)

    assertEquals(listOf(HttpMethod.Post, HttpMethod.Patch, HttpMethod.Delete), requests.map { it.method })
    assertEquals(listOf("uploaded 1", "edited 2", "purged 3"), outbox.log)
  }

  @Test
  fun `signed out sends nothing and keeps the note pending`() = runTest {
    outbox.notes += NOTE

    uploader(token = null).upload(UUID, WARSAW)

    assertTrue(requests.isEmpty())
    assertEquals(listOf(NOTE), outbox.pending(UUID))
  }

  private fun uploader(status: HttpStatusCode = HttpStatusCode.Created, token: String? = "tok-123"): MobileNoteUploader {
    val engine = MockEngine { request ->
      requests += request
        when {
        status != HttpStatusCode.Created -> respondError(status)
        request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
        else -> respond(CREATED_NOTE, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
      }
    }
    return MobileNoteUploader(
      api = MobileSessionsApi(createAircastingHttpClient(engine)),
      outbox = outbox,
      auth = StoredAuthSession(MapSettings()).apply { token?.let { start(it, AccountProfile("u", "u@example.com")) } },
    )
  }

  private companion object {
    const val UUID = "8d7a3f2e-0000-4000-8000-000000000001"
    val WARSAW = TimeZone.of("Europe/Warsaw")
    val NOTE = Note(1, UUID, Instant.parse("2026-10-19T08:00:05Z"), "Smells like smoke", GeoLocation(52.23, 21.01))
    val CREATED_NOTE = """
      {"id":17,"number":0,"text":"Smells like smoke","date":"2026-10-19T10:00:05",
       "latitude":52.23,"longitude":21.01,"photo_location":null}
    """.trimIndent()
  }
}
