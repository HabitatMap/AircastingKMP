package pl.llp.aircasting.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** `POST /api/v3/mobile_sessions/{uuid}/notes` as the v3 spec describes it: JSON, photo as base64. */
class MobileSessionsNotesApiTest {

  private val requests = mutableListOf<HttpRequestData>()

  private val api = MobileSessionsApi(
    createAircastingHttpClient(
      MockEngine { request ->
        requests += request
        respond(CreatedNote, HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
      }
    )
  )

  @Test
  fun `addNote posts the note fields as top level JSON`() = runTest {
    api.addNote("tok-123", UUID, CreateNoteRequestDto("Smells like smoke", "2026-08-14T10:00:00", 40.5, -74.25))

    val request = requests.single()
    assertEquals(HttpMethod.Post, request.method)
    assertEquals("/api/v3/mobile_sessions/$UUID/notes", request.url.encodedPath)
    assertEquals("Bearer tok-123", request.headers[HttpHeaders.Authorization])
    val content = request.body as TextContent
    assertEquals(ContentType.Application.Json, content.contentType.withoutParameters())
    val body = Json.parseToJsonElement(content.text).jsonObject
    assertEquals("Smells like smoke", body.getValue("text").jsonPrimitive.content)
    assertEquals("2026-08-14T10:00:00", body.getValue("date").jsonPrimitive.content)
    assertEquals(40.5, body.getValue("latitude").jsonPrimitive.content.toDouble())
    assertEquals(-74.25, body.getValue("longitude").jsonPrimitive.content.toDouble())
  }

  @Test
  fun `addNote without a photo leaves the photo key out`() = runTest {
    api.addNote("tok-123", UUID, CreateNoteRequestDto("No photo", "2026-08-14T10:00:00", 40.5, -74.25))

    val body = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject
    assertFalse("photo" in body)
  }

  @Test
  fun `addNote sends a photo as a base64 string`() = runTest {
    api.addNote("tok-123", UUID, CreateNoteRequestDto("With photo", "2026-08-14T10:00:00", 40.5, -74.25, photo = "aGVsbG8="))

    val body = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject
    assertEquals("aGVsbG8=", body.getValue("photo").jsonPrimitive.content)
  }

  @Test
  fun `addNote returns the server note with its id`() = runTest {
    val note = api.addNote("tok-123", UUID, CreateNoteRequestDto("Smells like smoke", "2026-08-14T10:00:00", 40.5, -74.25))

    assertEquals(17L, note.id)
    assertEquals("Smells like smoke", note.text)
  }

  private companion object {
    const val UUID = "8d7a3f2e-0000-4000-8000-000000000001"
    val CreatedNote = """
      {"id":17,"number":0,"text":"Smells like smoke","date":"2026-08-14T10:00:00",
       "latitude":40.5,"longitude":-74.25,"photo_location":null}
    """.trimIndent()
  }
}
