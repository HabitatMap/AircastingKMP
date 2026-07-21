package com.lunarlogic.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FixedStationsQuery(
  @SerialName("time_from") val timeFrom: String,
  @SerialName("time_to") val timeTo: String,
  @SerialName("tags") val tags: String = "",
  @SerialName("usernames") val usernames: String = "",
  @SerialName("west") val west: Double,
  @SerialName("east") val east: Double,
  @SerialName("south") val south: Double,
  @SerialName("north") val north: Double,
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  @SerialName("measurement_type") val measurementType: String,
)