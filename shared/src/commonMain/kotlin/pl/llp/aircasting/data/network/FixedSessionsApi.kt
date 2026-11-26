package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Instant

class FixedSessionsApi(private val client: HttpClient) {
  /**
   * The user's own fixed sessions, newest first. 500 is the server's page maximum. A full page took 11 s
   * for an account with 212 sessions, past OkHttp's default 10 s read timeout, hence [LIST_TIMEOUT_MS].
   */
  suspend fun listSessions(userToken: String, page: Int = 1, perPage: Int = 500): FixedSessionListResponseDto =
    client.get(ApiEndpoints.V3FixedSessions) {
      aircastingBearerAuth(userToken)
      parameter("page", page)
      parameter("per_page", perPage)
      timeout {
        requestTimeoutMillis = LIST_TIMEOUT_MS
        socketTimeoutMillis = LIST_TIMEOUT_MS
      }
    }
      .body()

  /** One of the user's own fixed sessions: the same shape as a [listSessions] entry. */
  suspend fun getSession(userToken: String, uuid: String): FixedSessionDto =
    client.get(ApiEndpoints.v3FixedSession(uuid)) { aircastingBearerAuth(userToken) }.body()

  /** One stream's readings. Both bounds inclusive, true UTC, at most 12 h apart. Oldest first. */
  suspend fun measurements(
    userToken: String, uuid: String, sensorName: String, start: Instant, end: Instant,
  ): List<StationMeasurementDto> = client.get(ApiEndpoints.v3FixedSessionMeasurements(uuid)) {
    aircastingBearerAuth(userToken)
    parameter("sensor_name", sensorName)
    parameter("start_time", start.toEpochMilliseconds())
    parameter("end_time", end.toEpochMilliseconds())
  }.body()

  /** Decommissions the monitor: the server stamps `finished_at` and drops later uploads. */
  suspend fun finishSession(userToken: String, uuid: String): FinishedFixedSessionDto =
    client.post(ApiEndpoints.v3FixedSessionFinish(uuid)) { aircastingBearerAuth(userToken) }.body()

  /** Create a fixed session; the response carries the token the AirBeam uploads with. */
  suspend fun createSession(
    userToken: String,
    request: CreateFixedSessionRequestDto,
  ): CreateFixedSessionResponseDto = client.post(ApiEndpoints.V3FixedSessions) {
    aircastingBearerAuth(userToken)
    contentType(ContentType.Application.Json)
    setBody(request)
  }
    .body()

  /** Replace the session's tags (space-separated); create takes none. */
  suspend fun updateTags(userToken: String, uuid: String, tagList: String) {
    client.patch(ApiEndpoints.v3FixedSession(uuid)) {
      aircastingBearerAuth(userToken)
      contentType(ContentType.Application.Json)
      setBody(UpdateFixedSessionRequestDto(tagList))
    }
  }

  private companion object {
    const val LIST_TIMEOUT_MS = 20_000L
  }
}
