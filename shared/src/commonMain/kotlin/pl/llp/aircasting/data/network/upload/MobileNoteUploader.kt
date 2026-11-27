package pl.llp.aircasting.data.network.upload

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.CreateNoteRequestDto
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteOutbox
import kotlin.io.encoding.Base64
import kotlin.time.Instant

/**
 * Sends a session's note changes made on this phone: creates (oldest first, recording each server id),
 * then edits, then deletes. Creates first, so a note deleted while its POST was in flight has a server
 * id to DELETE by then. Runs after the session is created on the server. One request per change: a
 * failure throws, what was sent stays sent and the rest stays pending for the next pass.
 */
class MobileNoteUploader(
  private val api: MobileSessionsApi,
  private val outbox: NoteOutbox,
  private val auth: AuthSession,
) {
  /** [timeZone]: the session's — the note's `date` is that wall clock, like the session's times. */
  suspend fun upload(sessionUuid: String, timeZone: TimeZone) {
    val token = auth.state.value.tokenOrNull ?: return
    for (note in outbox.pending(sessionUuid)) {
      val photo = if (note.hasPhoto) outbox.photo(note.id)?.let { Base64.encode(it) } else null
      val created = api.addNote(token, sessionUuid, note.toRequest(timeZone, photo))
      outbox.markUploaded(note.id, created.id, sentText = note.text)
    }
    for (edit in outbox.pendingEdits(sessionUuid)) {
      val remoteId = edit.remoteId ?: continue
      // 404: deleted on the web meanwhile. Nothing to edit; retrying would never succeed.
      ignoringNotFound { api.editNote(token, sessionUuid, remoteId, edit.text) }
      outbox.markEditUploaded(edit.id, sentText = edit.text)
    }
    for (gone in outbox.pendingDeletes(sessionUuid)) {
      // No server id: it never got there. 404: already deleted there. Either way, done.
      gone.remoteId?.let { remoteId -> ignoringNotFound { api.deleteNote(token, sessionUuid, remoteId) } }
      outbox.purge(gone.id)
    }
  }

  private suspend fun ignoringNotFound(request: suspend () -> Unit) {
    try {
      request()
    } catch (e: ClientRequestException) {
      if (e.response.status != HttpStatusCode.NotFound) throw e
    }
  }
}

private fun Note.toRequest(timeZone: TimeZone, photo: String?) = CreateNoteRequestDto(
  text = text,
  date = wallClock(time, timeZone),
  latitude = location.latitude,
  longitude = location.longitude,
  photo = photo,
)

/** `2026-10-19T10:00:00`: no zone, always whole seconds. LocalDateTime.toString() drops `:00` seconds. */
private fun wallClock(instant: Instant, zone: TimeZone): String = WallClock.format(instant.toLocalDateTime(zone))

private val WallClock = LocalDateTime.Format {
  year(); char('-'); monthNumber(); char('-'); day()
  char('T')
  hour(); char(':'); minute(); char(':'); second()
}
