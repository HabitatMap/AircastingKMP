package com.lunarlogic.aircasting.domain

import kotlin.time.Instant

data class GeoLocation(val latitude: Double, val longitude: Double)

data class FixedStation(
  val id: Long,
  val uuid: String,
  val name: String,
  val location: GeoLocation,
  val isIndoor: Boolean,
  val sensorName: String,
  val unitSymbol: String,
  val value: Double,
  val threshold: SensorThreshold,
  val updatedAt: Instant,
) {
  val level: MeasurementLevel get() = threshold.levelFor(value)
}