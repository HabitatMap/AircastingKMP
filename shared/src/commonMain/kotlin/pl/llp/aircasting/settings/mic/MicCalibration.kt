package pl.llp.aircasting.settings.mic

import kotlin.math.round

const val DefaultCalibrationOffset = 100
val CalibrationOffsetRange = 60..140
private const val QuietDb = 20.0
private const val LoudDb = 100.0

fun calibrate(dbFs: Double, offset: Int): Double = dbFs + offset

fun Int.steppedBy(delta: Int): Int = (this + delta).coerceIn(CalibrationOffsetRange)

fun loudnessFraction(db: Double): Float =
  ((db - QuietDb) / (LoudDb - QuietDb)).coerceIn(0.0, 1.0).toFloat()

fun formatReading(db: Double): String = (round(db * 10) / 10.0).toString()