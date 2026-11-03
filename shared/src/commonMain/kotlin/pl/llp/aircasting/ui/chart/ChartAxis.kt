package pl.llp.aircasting.ui.chart

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.domain.SensorThreshold
import kotlin.math.roundToInt
import kotlin.time.Instant

private const val SECONDS_PER_HOUR = 3_600

/** `HH:mm` of an epoch-seconds x. UTC on purpose: Explore times are local-as-UTC (DOMAIN_KNOWLEDGE.md). */
internal fun hourMinuteUtc(x: Double): String {
  val time = Instant.fromEpochSeconds(x.toLong()).toLocalDateTime(TimeZone.UTC)
  return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** Tooltip text, as the design's `01:00 · 13 µg/m³`. */
internal fun markerLabel(x: Double, y: Double, unit: String): String = "${hourMinuteUtc(x)} · ${y.roundToInt()} $unit"

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