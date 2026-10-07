package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class FixedSessionWithStreamsDto(
  val id: Long,
  val title: String,
  val username: String? = null,
  @SerialName("start_time") val startTime: Long? = null,
  @SerialName("end_time") val endTime: Long? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @SerialName("is_indoor") val isIndoor: Boolean = false,
  val streams: List<FixedStreamDetailsDto> = emptyList(),
)

@Serializable
data class FixedStreamDetailsDto(
  @SerialName("stream_id") val streamId: Long,
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("sensor_unit") val sensorUnit: String,
  @SerialName("unit_name") val unitName: String? = null,
  @SerialName("measurement_type") val measurementType: String,
  @SerialName("measurement_short_type") val measurementShortType: String? = null,
  @SerialName("last_measurement_value") val lastMeasurementValue: Double? = null,
  @SerialName("threshold_very_low") val thresholdVeryLow: Double = 0.0,
  @SerialName("threshold_low") val thresholdLow: Double = 0.0,
  @SerialName("threshold_medium") val thresholdMedium: Double = 0.0,
  @SerialName("threshold_high") val thresholdHigh: Double = 0.0,
  @SerialName("threshold_very_high") val thresholdVeryHigh: Double = 0.0,
  val measurements: List<FixedMeasurementDto> = emptyList(),
)

@Serializable
data class FixedMeasurementDto(
  val value: Double,
  val time: Long, // Epoch ms
  val latitude: Double? = null,
  val longitude: Double? = null,
)


/**
 * Accesses active fixed stations in a region and fixed session stream/measurement details.
 * Public endpoints (@NonAuthenticated) used by the Home screen and map queries.
 */
class FixedStationsApi(private val client: HttpClient) {
  /** Active fixed sessions in a bounding box for one sensor triple. */
  suspend fun activeInRegion(query: FixedStationsQuery): SessionsInRegionDto =
    client.get(ApiEndpoints.FixedStationsInRegion) {
      parameter("q", AircastingJson.encodeToString(query))
    }
      .body()

  /** Fixed session with all streams and measurement history (for weekly charts). */
  suspend fun sessionStreams(
    sessionId: Long,
    measurementsLimit: Int = 168,
  ): FixedSessionWithStreamsDto = client.get(ApiEndpoints.fixedSessionStreams(sessionId)) {
    parameter("measurements_limit", measurementsLimit)
  }
    .body()

  /** Single fixed stream with measurement history. */
  suspend fun stream(streamId: Long): FixedStreamDetailsDto =
    client.get(ApiEndpoints.fixedStream(streamId))
      .body()
}