package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `/explore/stations/{uuid}`. Unused fields (`is_active`, `share_url`, `time_zone`, location…) are dropped by `ignoreUnknownKeys`. */
@Serializable
data class StationDetailsDto(
  val source: String,
  val title: String,
  val provider: String,
  /** Null for gov, and for an AirBeam whose model the server doesn't recognise. */
  @SerialName("device_model") val deviceModel: String? = null,
  val streams: List<StationStreamDto>,
)

@Serializable
data class StationStreamDto(
  @SerialName("stream_id") val streamId: Long,
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  /** Null: the stream never reported. */
  @SerialName("last_measurement") val lastMeasurement: ExploreLastMeasurementDto?,
  /** Null: nothing reported in the last 24 h (a station that went quiet still opens). */
  @SerialName("last_24h") val last24h: Last24hDto?,
)

@Serializable
data class Last24hDto(val min: Int, val max: Int)