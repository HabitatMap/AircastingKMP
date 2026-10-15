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
  suspend fun insertAll(measurements: List<Measurement>)
  fun observeRecent(streamId: Long, limit: Int): Flow<List<Measurement>>
  suspend fun lastTime(streamId: Long): Instant?
  suspend fun pendingUpload(sessionUuid: String, limit: Int): List<Measurement>
  suspend fun markUploaded(measurements: List<Measurement>)
}