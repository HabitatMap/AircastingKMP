package pl.llp.aircasting.ui.chart

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.domain.SensorThreshold
import kotlin.time.Instant

private const val SECONDS_PER_HOUR = 3_600
private const val SECONDS_PER_DAY = 86_400

/**
 * Midnights after [fromX] up to [toX], for the dashed day-change line (legacy GraphDataGenerator).
 * Plain multiples of a day: x is local-as-UTC, so UTC midnight is the station's midnight.
 */
internal fun midnightsBetween(fromX: Double, toX: Double): List<Double> {
  val first = (fromX.toLong() / SECONDS_PER_DAY + 1) * SECONDS_PER_DAY
  return (first..toX.toLong() step SECONDS_PER_DAY.toLong()).map { it.toDouble() }
}

/** `HH:mm` of an epoch-seconds x. UTC on purpose: Explore times are local-as-UTC (DOMAIN_KNOWLEDGE.md). */
internal fun hourMinuteUtc(x: Double): String {
  val time = Instant.fromEpochSeconds(x.toLong()).toLocalDateTime(TimeZone.UTC)
  return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** `HH:mm:ss` of an epoch-seconds x, for the marker tooltip. UTC for the same reason as [hourMinuteUtc]. */
internal fun hourMinuteSecondUtc(x: Double): String {
  val time = Instant.fromEpochSeconds(x.toLong()).toLocalDateTime(TimeZone.UTC)
  return "${time.hour.twoDigits()}:${time.minute.twoDigits()}:${time.second.twoDigits()}"
}

/** Offset from the first point to the next full hour: Vico's aligned placer counts label positions from minX. */
internal fun secondsToNextHour(minX: Double): Int {
  val intoHour = (minX.toLong() % SECONDS_PER_HOUR).toInt()
  return (SECONDS_PER_HOUR - intoHour) % SECONDS_PER_HOUR
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