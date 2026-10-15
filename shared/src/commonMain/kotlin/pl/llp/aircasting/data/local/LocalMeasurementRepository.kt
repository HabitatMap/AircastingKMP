package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.MeasurementQueries
import pl.llp.aircasting.data.local.db.Measurements
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementRepository
import kotlin.time.Instant

class LocalMeasurementRepository(
  private val queries: MeasurementQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MeasurementRepository {

  override suspend fun insertAll(measurements: List<Measurement>) = withContext(ioDispatcher) {
    queries.transaction {
      measurements.forEach {
        queries.insertMeasurement(
          stream_id = it.streamId,
          time = it.time,
          value_ = it.value,
          latitude = it.location?.latitude,
          longitude = it.location?.longitude,
        )
      }
    }
  }
  override fun observeRecent(streamId: Long, limit: Int): Flow<List<Measurement>> =
    queries.selectRecentByStream(streamId, limit.toLong())
      .asFlow()
      .mapToList(ioDispatcher)
      .map { rows -> rows.map { it.toDomain() } }

  override suspend fun lastTime(streamId: Long): Instant? = withContext(ioDispatcher) {
    queries.selectLastTimeByStream(streamId).executeAsOneOrNull()
  }

  override suspend fun pendingUpload(sessionUuid: String, limit: Int): List<Measurement> =
    withContext(ioDispatcher) {
      queries.selectPendingUpload(sessionUuid, limit.toLong())
        .executeAsList()
        .map { it.toDomain() }
    }


  /**
   * Marks exactly the rows handed back by `pendingUpload`, by natural key — not a
   * high-water-mark cursor, which would wrongly swallow measurements backfilled out of
   * order by a BLE sync.
   */
  override suspend fun markUploaded(measurements: List<Measurement>) = withContext(ioDispatcher) {
    queries.transaction {
      measurements.forEach { queries.markUploaded(it.streamId, it.time) }
    }
  }
}

private fun Measurements.toDomain() = Measurement(
  streamId = stream_id,
  time = time,
  value = value_,
  location = latitude?.let { lat -> longitude?.let { lon -> GeoLocation(lat, lon) } },
  uploaded = uploaded,
)