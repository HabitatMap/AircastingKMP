package pl.llp.aircasting.settings.mic

import kotlin.math.log10

private const val Max16Bit = 32_768.0
/**
 * Legacy fudge factor: without it a realistically-saturated input never quite reaches 0 dB,
 * because that would need every sample to be exactly -32768. Kept verbatim so readings stay
 * comparable with the ones already recorded by the old app.
 */
private const val Fudge = 0.6

private const val MinDb = -100.0
private const val MaxDb = 0.0

/**
 * Direct port of legacy
 * `SignalPower.calculatePowerDb`.
 */

fun powerDbFs(samples: ShortArray, count: Int = samples.size): Double? {
  if (count <= 0) return null
  var sum = 0.0
  var squareSum = 0.0
  for (i in 0 until count) {
    val v = samples[i].toDouble()
    sum += v
    squareSum += v * v
  }
  val power = (squareSum - sum * sum / count) / count / (Max16Bit * Max16Bit)
  val db = log10(power) * 10.0 + Fudge
  return db.takeIf { it > MinDb && it < MaxDb }
}