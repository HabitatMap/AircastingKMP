package pl.llp.aircasting.explore

import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.random.Random
import kotlin.time.Clock

/** One stream's recent measurements for the graph, in any order. x = epoch seconds, local-as-UTC. */
interface StationMeasurementsRepository {
  suspend fun measurements(streamId: Long): List<ChartPoint>
}

/**
 * Stand-in until the endpoint's shape is settled: 24 h at 1 Hz (86 400 points), the worst case the
 * graph must handle without lag. Seeded by stream id, so each chip shows its own, stable line.
 * Replace with the network implementation in Koin; nothing else changes.
 */
class SampleStationMeasurementsRepository(private val clock: Clock) : StationMeasurementsRepository {
  override suspend fun measurements(streamId: Long): List<ChartPoint> {
    val random = Random(streamId)
    val end = clock.now().epochSeconds
    var value = 20.0
    return List(SAMPLE_COUNT) { i ->
      value = (value + random.nextDouble(-1.5, 1.5)).coerceIn(0.0, 80.0)
      ChartPoint(x = (end - SAMPLE_COUNT + 1 + i).toDouble(), y = value)
    }
  }

  private companion object {
    const val SAMPLE_COUNT = 86_400
  }
}