package pl.llp.aircasting.domain.session

import kotlinx.coroutines.flow.Flow
import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Instant

/**
 * A user's note on one of their mobile sessions. [id] is local; [time] and [location] are those of
 * the session's reading the note was pinned to, so it lands on that point of the graph and map.
 * [hasPhoto]: the JPEG itself loads on demand (NoteRepository.photo), not with every list.
 */
data class Note(
  val id: Long,
  val sessionUuid: String,
  val time: Instant,
  val text: String,
  val location: GeoLocation,
  val hasPhoto: Boolean = false,
)

/** [note] seen on one stream: [value] = that stream's latest reading at or before the note's time, if any. */
data class NoteOnStream(val note: Note, val value: Double?)

interface NoteRepository {
  /** The session's notes, newest first. */
  fun observe(sessionUuid: String): Flow<List<Note>>

  /** [observe], each note with [sensorName]'s reading where it sits — for its card and graph marker. */
  fun observeOnStream(sessionUuid: String, sensorName: String): Flow<List<NoteOnStream>>

  /**
   * Pins [text] to the session's newest reading that has a location (legacy AddNoteBottomSheet).
   * [photo]: a JPEG, already downscaled (PhotoCapture).
   * @return the stored note, or null when no reading has a location yet — nothing is stored.
   */
  suspend fun add(sessionUuid: String, text: String, photo: ByteArray? = null): Note?

  suspend fun photo(noteId: Long): ByteArray?

  /** New text; the server gets it on the next upload pass. */
  suspend fun edit(noteId: Long, text: String)

  /** Hidden at once; deleted on the server on the next upload pass. */
  suspend fun delete(noteId: Long)
}

/** A note change to send: [remoteId] = the server's id, null if the note never reached the server. */
data class NoteChange(val id: Long, val remoteId: Long?, val text: String)

/** Note changes made on this phone that the server doesn't have yet — MobileNoteUploader's work. */
interface NoteOutbox {
  /** To create, oldest first. */
  suspend fun pending(sessionUuid: String): List<Note>
  /** The JPEG a create sends along, if the note has one. */
  suspend fun photo(noteId: Long): ByteArray?
  /** [sentText]: what the POST carried; an edit made meanwhile stays pending. */
  suspend fun markUploaded(noteId: Long, remoteId: Long, sentText: String)
  /** Created notes whose text changed since. */
  suspend fun pendingEdits(sessionUuid: String): List<NoteChange>
  suspend fun markEditUploaded(noteId: Long, sentText: String)
  /** Deleted notes, uploaded or not. */
  suspend fun pendingDeletes(sessionUuid: String): List<NoteChange>
  /** Drops the row for good, once the server no longer has the note. */
  suspend fun purge(noteId: Long)
  /** Uuids of sessions with at least one pending note. */
  fun observeSessionsWithPending(): Flow<List<String>>
}
