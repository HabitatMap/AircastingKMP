package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExploreMapResponseDto(val items: List<ExploreMapItemDto>)

/** One map marker. The JSON `type` field picks the subclass via each one's @SerialName. */
@Serializable
sealed interface ExploreMapItemDto {
  val latitude: Double
  val longitude: Double
}

@Serializable
@SerialName("station")
data class ExploreMapStationDto(
  val uuid: String,
  val source: String,
  val title: String,
  val provider: String,
  override val latitude: Double,
  override val longitude: Double,
  /** One per selected measurement type this item reports. */
  val streams: List<ExploreMapStreamDto>,
) : ExploreMapItemDto
@Serializable
data class ExploreMapStreamDto(
  /** `streams.id` (AirBeam) or `station_streams.id` (gov): unique only within a source. */
  @SerialName("stream_id") val streamId: Long,
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("measurement_type") val measurementType: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  @SerialName("last_measurement") val lastMeasurement: ExploreLastMeasurementDto,
)

/** [value] rounded by the server; [time] epoch ms, true UTC. */
@Serializable
data class ExploreLastMeasurementDto(val value: Int, val time: Long)
@Serializable
@SerialName("cluster")
data class ExploreMapClusterDto(
  override val latitude: Double,
  override val longitude: Double,
  val count: Int,
  val value: Int? = null,
) : ExploreMapItemDto