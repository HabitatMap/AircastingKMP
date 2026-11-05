package pl.llp.aircasting.domain

import kotlin.math.abs
import kotlin.math.round

/** The three bounds the slider drags. The ends (veryLow, veryHigh) are only typed in, in the Thresholds sheet. */
enum class InnerBound { Low, Medium, High }

data class SensorThreshold(
  val sensorName: String,
  val veryLow: Int,
  val low: Int,
  val medium: Int,
  val high: Int,
  val veryHigh: Int,
) {
  fun levelFor(value: Double): MeasurementLevel {
    val v = round(value)
    return when {
      v > veryHigh -> MeasurementLevel.EXTREMELY_HIGH
      v > high -> MeasurementLevel.VERY_HIGH
      v > medium -> MeasurementLevel.HIGH
      v > low -> MeasurementLevel.MEDIUM
      v >= veryLow -> MeasurementLevel.LOW
      else -> MeasurementLevel.EXTREMELY_LOW
    }
  }
  /** The four coloured bands between the five bounds, low to high: what the chart and the slider paint. */
  val bands: List<Pair<IntRange, MeasurementLevel>>
    get() = listOf(
      veryLow..low to MeasurementLevel.LOW,
      low..medium to MeasurementLevel.MEDIUM,
      medium..high to MeasurementLevel.HIGH,
      high..veryHigh to MeasurementLevel.VERY_HIGH,
    )

  operator fun get(bound: InnerBound): Int = when (bound) {
    InnerBound.Low -> low
    InnerBound.Medium -> medium
    InnerBound.High -> high
  }

  /** [bound] moved to [to], kept strictly between its neighbours (legacy HLUDialog: min < low < medium < high < max). */
  fun moveBound(bound: InnerBound, to: Int): SensorThreshold = when (bound) {
    InnerBound.Low -> copy(low = to.strictlyBetween(veryLow, medium))
    InnerBound.Medium -> copy(medium = to.strictlyBetween(low, high))
    InnerBound.High -> copy(high = to.strictlyBetween(medium, veryHigh))
  } /** The bound a touch at [value] grabs. */
  fun nearestBound(value: Int): InnerBound = InnerBound.entries.minBy { abs(this[it] - value) }
}

// Not coerceIn: that throws on an empty range, and a server could send out-of-order bounds.
private fun Int.strictlyBetween(lower: Int, upper: Int) = coerceAtMost(upper - 1).coerceAtLeast(lower + 1)
