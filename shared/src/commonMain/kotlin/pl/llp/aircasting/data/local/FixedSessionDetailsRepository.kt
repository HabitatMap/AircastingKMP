package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.active.SessionDetailsRepository
import pl.llp.aircasting.data.network.FixedSessionsSync
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.ui.chart.ChartPoint

/**
 * Our own fixed session's details: the DB, like a mobile session's, plus one sync from the backend per
 * subscription (header: the session; graph: that stream's last 24 h). The stored values show at once;
 * the synced ones arrive through the same DB flow.
 */
class FixedSessionDetailsRepository(
  private val local: SessionDetailsRepository,
  private val sync: FixedSessionsSync,
) : SessionDetailsRepository by local {
  override fun observe(uuid: String): Flow<StationDetails?> =
    merge(local.observe(uuid), flow<Nothing> { sync.syncSession(uuid) })

  override fun observePoints(uuid: String, sensorName: String, timeZone: TimeZone): Flow<List<ChartPoint>> =
    merge(local.observePoints(uuid, sensorName, timeZone), flow<Nothing> { sync.syncMeasurements(uuid, sensorName) })
}
