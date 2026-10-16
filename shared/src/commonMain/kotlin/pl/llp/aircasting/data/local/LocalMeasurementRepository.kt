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
import pl.llp.aircasting.data.local.db.PendingUploadQueries
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementRepository
import kotlin.time.Instant

class LocalMeasurementRepository(
  private val queries: MeasurementQueries,
  private val outboxQueries: PendingUploadQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MeasurementRepository {

  override suspend fun insertAll(measurements: List<Measurement>): Int =
    insert(measurements, enqueue = false)

  override suspend fun insertRecorded(measurements: List<Measurement>): Int =
    insert(measurements, enqueue = true)

  private suspend fun insert(measurements: List<Measurement>, enqueue: Boolean): Int =
    withContext(ioDispatcher) {
      queries.transactionWithResult {
        measurements.count { m ->
          val stored = queries.insertMeasurement(
            stream_id = m.streamId,
            time = m.time,
            value_ = m.value,
            latitude = m.location?.latitude,
            longitude = m.location?.longitude,
          ).value == 1L
          if (stored && enqueue) outboxQueries.enqueue(m.streamId, m.time)
          stored
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
}

internal fun Measurements.toDomain() = Measurement(
  streamId = stream_id,
  time = time,
  value = value_,
  location = latitude?.let { lat -> longitude?.let { lon -> GeoLocation(lat, lon) } },
)