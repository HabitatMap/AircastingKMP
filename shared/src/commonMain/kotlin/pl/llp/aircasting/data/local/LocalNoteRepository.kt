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
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.domain.session.NoteRepository

class LocalNoteRepository(
  private val queries: NoteQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NoteRepository {

  override fun observe(sessionUuid: String): Flow<List<Note>> =
    queries.selectBySession(sessionUuid) { id, uuid, time, text, latitude, longitude, hasPhoto ->
      Note(id, uuid, time, text, GeoLocation(latitude, longitude), hasPhoto)
    }
      .asFlow()
      .mapToList(ioDispatcher)

  override fun observeOnStream(sessionUuid: String, sensorName: String): Flow<List<NoteOnStream>> =
    queries.selectBySessionOnStream(sensorName, sessionUuid) { id, uuid, time, text, latitude, longitude, hasPhoto, value ->
      NoteOnStream(Note(id, uuid, time, text, GeoLocation(latitude, longitude), hasPhoto), value)
    }
      .asFlow()
      .mapToList(ioDispatcher)

  override suspend fun edit(noteId: Long, text: String) {
    withContext(ioDispatcher) { queries.updateText(text = text, id = noteId) }
  }

  override suspend fun delete(noteId: Long) {
    withContext(ioDispatcher) { queries.markDeleted(noteId) }
  }

  // One transaction: the reading we pin to and the insert see the same DB state.
  override suspend fun photo(noteId: Long): ByteArray? = withContext(ioDispatcher) {
    queries.selectPhoto(noteId).executeAsOneOrNull()?.photo
  }

  override suspend fun add(sessionUuid: String, text: String, photo: ByteArray?): Note? = withContext(ioDispatcher) {
    queries.transactionWithResult {
      val reading = queries.selectNewestLocatedReading(sessionUuid).executeAsOneOrNull()
        ?: return@transactionWithResult null
      // The query's WHERE guarantees both; SQLDelight still types the columns as nullable.
      val location = GeoLocation(reading.latitude!!, reading.longitude!!)
      queries.insertNote(sessionUuid, reading.time, text, location.latitude, location.longitude, photo)
      Note(queries.lastInsertedId().executeAsOne(), sessionUuid, reading.time, text, location, hasPhoto = photo != null)
    }
  }
}
