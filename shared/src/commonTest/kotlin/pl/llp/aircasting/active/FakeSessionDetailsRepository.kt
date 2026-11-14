package pl.llp.aircasting.active

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.ui.chart.ChartPoint

data class PointsRequest(val uuid: String, val sensorName: String, val timeZone: TimeZone)

/** Live like the DB: set [details]/[points] to emit. [failPoints] makes the next observePoints throw. */
class FakeSessionDetailsRepository(
  val details: MutableStateFlow<StationDetails?> = MutableStateFlow(null),
  val points: MutableStateFlow<List<ChartPoint>> = MutableStateFlow(emptyList()),
  var failPoints: Throwable? = null,
) : SessionDetailsRepository {
  val pointRequests = mutableListOf<PointsRequest>()

  override fun observe(uuid: String): Flow<StationDetails?> = details

  override fun observePoints(uuid: String, sensorName: String, timeZone: TimeZone): Flow<List<ChartPoint>> {
    pointRequests += PointsRequest(uuid, sensorName, timeZone)
    return failPoints?.let { e -> flow { throw e } } ?: points
  }
}