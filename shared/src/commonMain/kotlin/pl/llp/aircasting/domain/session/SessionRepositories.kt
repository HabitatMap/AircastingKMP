package pl.llp.aircasting.domain.session

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface SessionRepository {
  suspend fun insert(session: Session)
  suspend fun find(uuid: String): Session?
  fun observe(uuid: String): Flow<Session?>
  fun observeAll(): Flow<List<Session>>
  suspend fun updateStatus(uuid: String, status: SessionStatus)
  suspend fun finish(uuid: String, endTime: Instant)
  suspend fun delete(uuid: String)
}

interface MeasurementStreamRepository {
  suspend fun findOrCreate(
    sessionUuid: String,
    sensorName: String,
    unitSymbol: String,
    sensorTypeId: Int? = null,
  ): Long

  suspend fun findBySession(sessionUuid: String): List<MeasurementStream>
  fun observeBySession(sessionUuid: String): Flow<List<MeasurementStream>>
  suspend fun setSensorTypeId(streamId: Long, sensorTypeId: Int)
}

interface MeasurementRepository {
  /**
   * Data that originated elsewhere — downloaded, followed, explored, fixed. Stored only;
   * never becomes upload work.
   * @return how many rows were newly stored; the rest already existed for (stream, time).
   */
  suspend fun insertAll(measurements: List<Measurement>): Int
  /**
   * Data recorded on this phone. Stores AND enqueues for upload, in one transaction.
   * Only newly stored rows are enqueued — an OR IGNORE-skipped duplicate was enqueued
   * when it was first recorded.
   * @return how many rows were newly stored.
   */
  suspend fun insertRecorded(measurements: List<Measurement>): Int
  /** Newest-first window. Replaces the old `active_sessions_measurements` table. */
  fun observeRecent(streamId: Long, limit: Int): Flow<List<Measurement>>
  suspend fun lastTime(streamId: Long): Instant?
}