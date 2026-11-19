package pl.llp.aircasting.ui.chart

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.domain.SensorThreshold
import kotlin.time.Instant

private const val SECONDS_PER_DAY = 86_400

/**
 * Midnights after [fromX] up to [toX], for the dashed day-change line (legacy GraphDataGenerator).
 * Plain multiples of a day: x is local-as-UTC, so UTC midnight is the station's midnight.
 */
internal fun midnightsBetween(fromX: Double, toX: Double): List<Double> {
  val first = (fromX.toLong() / SECONDS_PER_DAY + 1) * SECONDS_PER_DAY
  return (first..toX.toLong() step SECONDS_PER_DAY.toLong()).map { it.toDouble() }
}

/**
 * `HH:mm:ss` of an epoch-seconds x, for the axis labels and the marker tooltip. UTC on purpose:
 * Explore times are local-as-UTC (DOMAIN_KNOWLEDGE.md).
 */
internal fun hourMinuteSecondUtc(x: Double): String {
  val time = Instant.fromEpochSeconds(x.toLong()).toLocalDateTime(TimeZone.UTC)
  return "${time.hour.twoDigits()}:${time.minute.twoDigits()}:${time.second.twoDigits()}"
}

/**
 * The x-axis labels for [visible], as label-centre x → the time that label shows. [labelWidth] is
 * in x units (seconds), the widest label's.
 *
 * One label flush inside each edge, showing the exact time there; nothing in between, so every
 * graph reads the same whatever span it shows. Ordered by x, as Vico expects.
 */
internal fun timeAxisLabels(visible: ClosedFloatingPointRange<Double>, labelWidth: Double): Map<Double, Double> {
  val left = visible.start + labelWidth / 2
  val right = visible.endInclusive - labelWidth / 2
  if (right - left < labelWidth) return if (left <= right) mapOf(left to visible.start) else emptyMap()
  return linkedMapOf(left to visible.start, right to visible.endInclusive)
}

/**
 * Fixed y range for the whole series. It must not follow the loaded window, or the axis would
 * rescale while panning and the bands would slide under the line.
 */
internal fun yRange(points: List<ChartPoint>, threshold: SensorThreshold?): ClosedFloatingPointRange<Double> {
  val max = points.maxOf { it.y }
  if (threshold != null) return threshold.veryLow.toDouble()..maxOf(threshold.veryHigh.toDouble(), max)
  val min = points.minOf { it.y }
  return if (min == max) (min - 1)..(max + 1) else min..max
}

private fun Int.twoDigits() = toString().padStart(2, '0')