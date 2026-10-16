package pl.llp.aircasting.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.PendingUploadQueries
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.UploadOutbox

class LocalUploadOutbox(
  private val queries: PendingUploadQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UploadOutbox {

  override suspend fun pending(sessionUuid: String, limit: Int): List<Measurement> =
    withContext(ioDispatcher) {
      queries.selectPending(sessionUuid, limit.toLong()).executeAsList().map { it.toDomain() }
    }

  /**
   * Deletes by natural key, exactly the rows that were sent — correct even when a BLE sync
   * backfilled older timestamps after newer ones went out.
   */
  override suspend fun acknowledge(measurements: List<Measurement>): Unit =
    withContext(ioDispatcher) {
      queries.transaction {
        measurements.forEach { queries.acknowledge(it.streamId, it.time) }
      }
    }
}