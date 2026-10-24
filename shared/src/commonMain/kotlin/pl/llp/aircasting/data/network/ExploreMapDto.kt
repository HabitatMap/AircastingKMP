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
  val value: Int?
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
  @SerialName("last_measured_at") val lastMeasuredAt: Long, // Epoch ms, UTC
  @SerialName("measurement_types") val measurementTypes: List<String>,
  override val value: Int? = null,
) : ExploreMapItemDto


@Serializable
@SerialName("cluster")
data class ExploreMapClusterDto(
  override val latitude: Double,
  override val longitude: Double,
  val count: Int,
  override val value: Int? = null,
) : ExploreMapItemDto