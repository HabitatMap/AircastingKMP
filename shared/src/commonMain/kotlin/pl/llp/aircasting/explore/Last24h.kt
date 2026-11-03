package pl.llp.aircasting.explore

import pl.llp.aircasting.ui.chart.ChartPoint

private const val DAY_SECONDS = 86_400.0

/**
 * The station graph's points: sorted, then the 24 h ending at the newest point — not at "now", so
 * a station that went quiet still shows its last day. Legacy
 * `SearchFollowViewModel.getStreamsWithMeasurementsFromResponse` (`lastMeasurementTimeMinus24Hours`).
 */
internal fun last24h(points: List<ChartPoint>): List<ChartPoint> {
  val sorted = points.sortedBy { it.x }
  val from = (sorted.lastOrNull() ?: return sorted).x - DAY_SECONDS
  return sorted.dropWhile { it.x < from }
}