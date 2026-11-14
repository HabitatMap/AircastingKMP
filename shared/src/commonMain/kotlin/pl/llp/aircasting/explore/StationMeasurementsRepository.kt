package pl.llp.aircasting.explore

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.StationMeasurementDto
import pl.llp.aircasting.ui.chart.ChartPoint
import pl.llp.aircasting.ui.chart.wallClockPoint
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** One stream's measurements for the graph, in any order. x = epoch seconds, local-as-UTC. */
interface StationMeasurementsRepository {
  /** The 24 h ending at [newest], with x on [timeZone]'s wall clock. */
  suspend fun measurements(uuid: String, sensorName: String, newest: Instant, timeZone: TimeZone): List<ChartPoint>
}

class NetworkStationMeasurementsRepository(private val api: ExploreMapApi) : StationMeasurementsRepository {
  override suspend fun measurements(
    uuid: String, sensorName: String, newest: Instant, timeZone: TimeZone,
  ): List<ChartPoint> = coroutineScope {
    val split = newest - WINDOW
    // 12 h is the endpoint's max, and it includes both bounds: the older window stops 1 ms short
    // of the newer one, or the reading on the boundary would come back twice.
    val newer = async { api.stationMeasurements(uuid, sensorName, split, newest) }
    val older = async { api.stationMeasurements(uuid, sensorName, newest - WINDOW * 2, split - 1.milliseconds) }
    (older.await() + newer.await()).map { it.toChartPoint(timeZone) }
  }
  private companion object {
    val WINDOW = 12.hours
  }
}

private fun StationMeasurementDto.toChartPoint(timeZone: TimeZone): ChartPoint =
  wallClockPoint(Instant.fromEpochMilliseconds(time), value, timeZone)
