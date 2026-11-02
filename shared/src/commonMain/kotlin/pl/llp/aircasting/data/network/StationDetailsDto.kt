package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `/explore/stations/{uuid}`. Unused fields (`active`, `share_url`, location…) are dropped by `ignoreUnknownKeys`. */
@Serializable
data class StationDetailsDto(
  val source: String,
  val title: String,
  val provider: String,
  /** Nullable until BE confirms gov stations send it. */
  @SerialName("device_model") val deviceModel: String? = null,
  val streams: List<StationStreamDto>,
)

@Serializable
data class StationStreamDto(
  @SerialName("stream_id") val streamId: Long,
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  @SerialName("last_measurement") val lastMeasurement: ExploreLastMeasurementDto,
  @SerialName("last_24h") val last24h: Last24hDto,
)

@Serializable
data class Last24hDto(val min: Int, val max: Int)