package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class MobileSessionsApi(private val client: HttpClient) {
  /** List the signed-in user's mobile sessions, paginated. */
  suspend fun listSessions(
    userToken: String,
    page: Int = 1,
    perPage: Int = 500,
  ): MobileSessionListResponseDto = client.get(ApiEndpoints.V3MobileSessions) {
    aircastingBearerAuth(userToken)
    parameter("page", page)
    parameter("per_page", perPage)
  }
    .body()

  /** Create a mobile session before recording begins. */
  suspend fun createSession(
    userToken: String,
    request: CreateMobileSessionRequestDto,
  ): MobileSessionSummaryDto = client.post(ApiEndpoints.V3MobileSessions) {
    aircastingBearerAuth(userToken)
    contentType(ContentType.Application.Json)
    setBody(request)
  }
    .body()

  /** Fetch details for one mobile session by UUID. */
  suspend fun getSession(userToken: String, uuid: String): MobileSessionSummaryDto =
    client.get(ApiEndpoints.v3MobileSession(uuid)) {
      aircastingBearerAuth(userToken)
    }
      .body()

  /** Update title, tags, or crowd contribution status. */
  suspend fun updateSession(
    userToken: String,
    uuid: String,
    request: UpdateMobileSessionRequestDto,
  ): MobileSessionSummaryDto = client.patch(ApiEndpoints.v3MobileSession(uuid)) {
    aircastingBearerAuth(userToken)
    contentType(ContentType.Application.Json)
    setBody(request)
  }
    .body()

  /** Delete a mobile session. */
  suspend fun deleteSession(userToken: String, uuid: String) {
    client.delete(ApiEndpoints.v3MobileSession(uuid)) {
      aircastingBearerAuth(userToken)
    }
  }

  /** Upload recorded binary measurements for a mobile session. */
  suspend fun uploadMeasurementsBinary(
    userToken: String,
    uuid: String,
    binaryData: ByteArray,
  ) {
    client.post(ApiEndpoints.v3MobileSessionMeasurements(uuid)) {
      aircastingBearerAuth(userToken)
      contentType(ContentType.Application.OctetStream)
      setBody(binaryData)
    }
  }
  // --- Notes ---
  /** List all notes attached to a mobile session. */
  suspend fun listNotes(userToken: String, uuid: String): List<NoteDto> =
    client.get(ApiEndpoints.v3MobileSessionNotes(uuid)) {
      aircastingBearerAuth(userToken)
    }
      .body()

  /** Add a note, optionally attaching photo bytes. */
  suspend fun addNote(
    userToken: String,
    uuid: String,
    noteJson: String,
    photoBytes: ByteArray? = null,
  ): NoteDto = client.submitFormWithBinaryData(
    url = ApiEndpoints.v3MobileSessionNotes(uuid),
    formData = formData {
      append("note", noteJson, Headers.build {
        append(HttpHeaders.ContentType, "application/json")
      })
      if (photoBytes != null) {
        append("photo", photoBytes, Headers.build {
          append(HttpHeaders.ContentType, "image/jpeg")
          append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
        })
      }
    },
  ) {
    aircastingBearerAuth(userToken)
  }
    .body()

  /** Edit text of an existing note. */
  suspend fun editNote(
    userToken: String,
    uuid: String,
    noteId: Long,
    text: String,
  ): NoteDto = client.patch(ApiEndpoints.v3MobileSessionNote(uuid, noteId)) {
    aircastingBearerAuth(userToken)
    contentType(ContentType.Application.Json)
    setBody(mapOf("text" to text))
  }
    .body()

  /** Delete a note by ID. */
  suspend fun deleteNote(userToken: String, uuid: String, noteId: Long) {
    client.delete(ApiEndpoints.v3MobileSessionNote(uuid, noteId)) {
      aircastingBearerAuth(userToken)
    }
  }
}