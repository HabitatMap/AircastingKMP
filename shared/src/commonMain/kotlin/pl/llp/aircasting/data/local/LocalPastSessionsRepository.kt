package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.SessionQueries
import pl.llp.aircasting.data.local.db.Sessions
import pl.llp.aircasting.mydata.PastSession
import pl.llp.aircasting.mydata.PastSessionsRepository
import pl.llp.aircasting.mydata.StreamSummary

class LocalPastSessionsRepository(
  private val queries: SessionQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PastSessionsRepository {

  /**
   * Triggered by the sessions table only, on purpose: the stats query scans every past
   * measurement, and a recording session writes one per second — listening to the measurements
   * table would rerun that scan every second for data that can't have changed. A finished
   * session's measurements are frozen; the only change that matters (a session finishing,
   * renamed, deleted) touches the sessions row.
   * TODO: SD-card sync adds measurements to finished sessions — re-trigger when it lands.
   */
  override fun observe(): Flow<List<PastSession>> =
    queries.selectPastMobile()
      .asFlow()
      .map { withContext(ioDispatcher) { queries.transactionWithResult { snapshot() } } }
      .distinctUntilChanged()

  private fun snapshot(): List<PastSession> {
    val stats = queries.selectPastMobileStreamStats().executeAsList().groupBy(
      keySelector = { it.session_uuid },
      valueTransform = { StreamSummary(it.sensor_name, it.unit_symbol, it.avg_value, it.min_value, it.max_value) },
    )
    // `end_time IS NOT NULL` makes SQLDelight generate its own row type (end_time non-null);
    // ::Sessions maps it back to the table type, so the shared toDomain() applies.
    return queries.selectPastMobile(::Sessions).executeAsList().map { PastSession(it.toDomain(), stats[it.uuid].orEmpty()) }
  }
}
