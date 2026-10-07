package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ThresholdAlertDto(
  val id: Long? = null,
  @SerialName("sensor_name") val sensorName: String,
  val threshold: Int,
  val direction: String = "above",
  val email: Boolean = true,
  val notification: Boolean = true,
)

class ThresholdAlertsApi(private val client: HttpClient) {
  suspend fun alerts(userToken: String): List<ThresholdAlertDto> =
    client.get(ApiEndpoints.ThresholdAlerts) {
      aircastingBearerAuth(userToken)
    }
      .body()

  suspend fun createAlert(userToken: String, alert: ThresholdAlertDto): ThresholdAlertDto =
    client.post(ApiEndpoints.ThresholdAlerts) {
      aircastingBearerAuth(userToken)
      contentType(ContentType.Application.Json)
      setBody(alert)
    }
      .body()

  suspend fun deleteAlert(userToken: String, alertId: Long) {
    client.delete(ApiEndpoints.thresholdAlert(alertId)) {
      aircastingBearerAuth(userToken)
    }
  }
}