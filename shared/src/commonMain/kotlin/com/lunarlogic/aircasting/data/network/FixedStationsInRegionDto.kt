package com.lunarlogic.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SessionsInRegionDto(
  @SerialName("fetchableSessionsCount") val fetchableSessionsCount: Int = 0,
  @SerialName("sessions") val sessions: List<SessionInRegionDto> = emptyList(),
)

@Serializable
data class SessionInRegionDto(
  @SerialName("id") val id: Long,
  @SerialName("uuid") val uuid: String,
  @SerialName("title") val title: String,
  @SerialName("latitude") val latitude: Double,
  @SerialName("longitude") val longitude: Double,
  @SerialName("is_indoor") val isIndoor: Boolean = false,
  @SerialName("last_hour_average") val lastHourAverage: Double,
  @SerialName("end_time_local") val endTimeLocal: String,
  @SerialName("streams") val streams: StreamsDto,
)


@Serializable
data class StreamsDto(
  // Legacy Gson: @SerializedName("Sensor", alternate=[…]) — the JSON key varies by sensor source.
  @SerialName("Sensor")
  @JsonNames("AirBeam2-PM2.5", "AirBeam3-PM2.5", "PurpleAir-PM2.5", "OpenAQ-PM2.5", "OpenAQ-O3")
  val sensor: SensorDto,
)
@Serializable
data class SensorDto(
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("measurement_type") val measurementType: String,
  @SerialName("threshold_very_low") val thresholdVeryLow: Int,
  @SerialName("threshold_low") val thresholdLow: Int,
  @SerialName("threshold_medium") val thresholdMedium: Int,
  @SerialName("threshold_high") val thresholdHigh: Int,
  @SerialName("threshold_very_high") val thresholdVeryHigh: Int,
  @SerialName("last_measurement_value") val lastMeasurementValue: Double = 0.0,
  @SerialName("unit_symbol") @JsonNames("sensor_unit") val unitSymbol: String,
)