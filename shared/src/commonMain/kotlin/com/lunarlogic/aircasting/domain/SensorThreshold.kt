package com.lunarlogic.aircasting.domain

import kotlin.math.round

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
}