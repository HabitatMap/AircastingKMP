package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `measurement_type`, `unit_symbol` are dropped by `ignoreUnknownKeys`. */
@Serializable
data class ThresholdSetsDto(@SerialName("threshold_sets") val thresholdSets: List<ThresholdSetDto>)

@Serializable
data class ThresholdSetDto(
  @SerialName("sensor_name") val sensorName: String,
  /** Every spelling that resolves here; always contains [sensorName] itself. */
  val aliases: List<String>,
  val thresholds: ThresholdBoundsDto,
)

/** Spec types these as float; values are whole numbers today. */
@Serializable
data class ThresholdBoundsDto(
  @SerialName("very_low") val veryLow: Double,
  val low: Double,
  val medium: Double,
  val high: Double,
  @SerialName("very_high") val veryHigh: Double,
)