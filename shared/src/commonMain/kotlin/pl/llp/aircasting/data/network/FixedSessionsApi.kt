package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FixedSessionsApi(private val client: HttpClient) {
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
}