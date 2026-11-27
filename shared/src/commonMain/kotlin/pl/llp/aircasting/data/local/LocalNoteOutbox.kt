package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.NoteQueries
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteChange
import pl.llp.aircasting.domain.session.NoteOutbox

class LocalNoteOutbox(
  private val queries: NoteQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NoteOutbox {

  override suspend fun pending(sessionUuid: String): List<Note> = withContext(ioDispatcher) {
    queries.selectPending(sessionUuid) { id, uuid, time, text, latitude, longitude, hasPhoto ->
      Note(id, uuid, time, text, GeoLocation(latitude, longitude), hasPhoto)
    }.executeAsList()
  }

  override suspend fun photo(noteId: Long): ByteArray? = withContext(ioDispatcher) {
    queries.selectPhoto(noteId).executeAsOneOrNull()?.photo
  }

  override suspend fun markUploaded(noteId: Long, remoteId: Long, sentText: String) {
    withContext(ioDispatcher) { queries.markUploaded(remoteId = remoteId, sentText = sentText, id = noteId) }
  }

  override suspend fun pendingEdits(sessionUuid: String): List<NoteChange> = withContext(ioDispatcher) {
    queries.selectPendingEdits(sessionUuid, ::NoteChange).executeAsList()
  }

  override suspend fun markEditUploaded(noteId: Long, sentText: String) {
    withContext(ioDispatcher) { queries.markEditUploaded(id = noteId, sentText = sentText) }
  }

  override suspend fun pendingDeletes(sessionUuid: String): List<NoteChange> = withContext(ioDispatcher) {
    queries.selectPendingDeletes(sessionUuid, ::NoteChange).executeAsList()
  }

  override suspend fun purge(noteId: Long) {
    withContext(ioDispatcher) { queries.purge(noteId) }
  }

  override fun observeSessionsWithPending(): Flow<List<String>> =
    queries.selectSessionsWithPending().asFlow().mapToList(ioDispatcher)
}
