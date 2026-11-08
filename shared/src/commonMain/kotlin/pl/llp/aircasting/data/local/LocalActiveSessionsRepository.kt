package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.active.ActiveMobileSession
import pl.llp.aircasting.active.ActiveSessionsRepository
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.data.local.db.SessionQueries

class LocalActiveSessionsRepository(
  private val queries: SessionQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ActiveSessionsRepository {

  /**
   * The readings query joins all three tables, so it alone tells us when anything changed. Both reads
   * then run in one transaction: a session and its readings always come from the same snapshot.
   */
  override fun observeMobile(): Flow<List<ActiveMobileSession>> =
    queries.selectActiveMobileLatestReadings()
      .asFlow()
      .map { withContext(ioDispatcher) { queries.transactionWithResult { snapshot() } } }
      .distinctUntilChanged()

  private fun snapshot(): List<ActiveMobileSession> {
    val readings = queries.selectActiveMobileLatestReadings().executeAsList().groupBy(
      keySelector = { it.session_uuid },
      valueTransform = { LatestReading(it.sensor_name, it.unit_symbol, it.value_, it.time) },
    )
    return queries.selectActiveMobile().executeAsList().map {
      ActiveMobileSession(it.toDomain(), readings[it.uuid].orEmpty())
    }
  }
}
