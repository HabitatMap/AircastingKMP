package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import pl.llp.aircasting.active.ActiveFixedSession
import pl.llp.aircasting.active.ActiveFixedSessionsRepository
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.data.local.db.FixedSessionQueries
import pl.llp.aircasting.data.network.FixedSessionsSync
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The Active tab's fixed sessions, read from the DB: the stored list shows at once, offline or after a
 * restart. While collected it also syncs from the backend every [pollEvery] (the AirBeam uploads about
 * once a minute) and on [refresh]; each sync writes the DB, which re-emits.
 */
class LocalActiveFixedSessionsRepository(
  private val queries: FixedSessionQueries,
  private val sync: FixedSessionsSync,
  private val pollEvery: Duration = 60.seconds,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ActiveFixedSessionsRepository {
  /** DROP_OLDEST: taps while a sync runs collapse into one more sync. */
  private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  override fun observeFixed(): Flow<List<ActiveFixedSession>> = merge(stored(), syncing())

  override fun refresh() {
    refreshes.tryEmit(Unit)
  }

  override suspend fun finish(uuid: String) = sync.finish(uuid)

  /** Emits nothing: it only keeps the DB fresh while the tab is subscribed. */
  private fun syncing(): Flow<Nothing> = flow {
    merge(ticks(), refreshes).onStart { emit(Unit) }.collect { sync.syncList() }
  }

  private fun ticks() = flow {
    while (true) {
      delay(pollEvery)
      emit(Unit)
    }
  }

  /** Same shape as the mobile one: the readings query joins all three tables, so it alone signals changes. */
  private fun stored(): Flow<List<ActiveFixedSession>> =
    queries.selectActiveFixedLatestReadings()
      .asFlow()
      .map { withContext(ioDispatcher) { queries.transactionWithResult { snapshot() } } }
      .distinctUntilChanged()

  private fun snapshot(): List<ActiveFixedSession> {
    val readings = queries.selectActiveFixedLatestReadings().executeAsList().groupBy(
      keySelector = { it.session_uuid },
      valueTransform = { LatestReading(it.sensor_name, it.unit_symbol, it.value_, it.time) },
    )
    return queries.selectActiveFixed().executeAsList().map {
      ActiveFixedSession(
        uuid = it.uuid,
        name = it.name,
        tags = it.tags,
        deviceModel = it.device_model,
        isIndoor = it.is_indoor,
        timeZone = it.time_zone,
        startTime = it.start_time,
        lastReportedAt = it.last_reported_at,
        readings = readings[it.uuid].orEmpty(),
      )
    }
  }
}
