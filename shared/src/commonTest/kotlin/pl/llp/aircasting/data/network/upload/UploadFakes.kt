package pl.llp.aircasting.data.network.upload

import kotlinx.coroutines.flow.Flow
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementStream
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteChange
import pl.llp.aircasting.domain.session.NoteOutbox
import pl.llp.aircasting.domain.session.UploadOutbox

/** Mirrors LocalUploadOutbox: oldest first, at most [limit]; acknowledge deletes. */
internal class FakeUploadOutbox : UploadOutbox {
  val rows = mutableListOf<Measurement>()
  override suspend fun pending(sessionUuid: String, limit: Int) = rows.sortedBy { it.time }.take(limit)
  override suspend fun acknowledge(measurements: List<Measurement>) { rows -= measurements.toSet() }
  override fun observeSessionsWithPending(): Flow<List<String>> = error("unused")
}

internal class FakeStreamRepository(streams: List<MeasurementStream>) : MeasurementStreamRepository {
  val streams = streams.toMutableList()
  override suspend fun findBySession(sessionUuid: String) = streams.filter { it.sessionUuid == sessionUuid }
  override suspend fun setSensorTypeId(streamId: Long, sensorTypeId: Int) {
    val index = streams.indexOfFirst { it.id == streamId }
    streams[index] = streams[index].copy(sensorTypeId = sensorTypeId)
  }
  override suspend fun findOrCreate(sessionUuid: String, sensorName: String, unitSymbol: String, sensorTypeId: Int?): Long = error("unused")
  override fun observeBySession(sessionUuid: String): Flow<List<MeasurementStream>> = error("unused")
}
/**
 * Mirrors LocalNoteOutbox for one session: [notes] to create (until [uploaded] has their id), [edits]
 * and [deletes] until marked sent / purged. [log] records the outbox calls in order.
 */
internal class FakeNoteOutbox : NoteOutbox {
  val notes = mutableListOf<Note>()
  val uploaded = mutableMapOf<Long, Long>()
  val edits = mutableListOf<NoteChange>()
  val deletes = mutableListOf<NoteChange>()
  val log = mutableListOf<String>()
  val photos = mutableMapOf<Long, ByteArray>()
  override suspend fun photo(noteId: Long): ByteArray? = photos[noteId]
  override suspend fun pending(sessionUuid: String) =
    notes.filter { it.sessionUuid == sessionUuid && it.id !in uploaded }.sortedBy { it.time }
  override suspend fun markUploaded(noteId: Long, remoteId: Long, sentText: String) {
    uploaded[noteId] = remoteId
    log += "uploaded $noteId"
  }
  override suspend fun pendingEdits(sessionUuid: String) = edits.toList()
  override suspend fun markEditUploaded(noteId: Long, sentText: String) {
    edits.removeAll { it.id == noteId }
    log += "edited $noteId"
  }
  override suspend fun pendingDeletes(sessionUuid: String) = deletes.toList()
  override suspend fun purge(noteId: Long) {
    deletes.removeAll { it.id == noteId }
    log += "purged $noteId"
  }
  override fun observeSessionsWithPending(): Flow<List<String>> = error("unused")
}
