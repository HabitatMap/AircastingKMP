package pl.llp.aircasting.active

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.ui.chart.ChartPoint

/** One of our own sessions, read live from the local DB: re-emits on every new measurement. */
interface SessionDetailsRepository {
  /** Null = no such session (e.g. deleted). Streams in chip order. */
  fun observe(uuid: String): Flow<StationDetails?>
  /** One stream's readings for the graph, oldest first, x = epoch seconds on [timeZone]'s wall clock. */
  fun observePoints(uuid: String, sensorName: String, timeZone: TimeZone): Flow<List<ChartPoint>>
}
