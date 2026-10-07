package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ExportApi(private val client: HttpClient) {
  /** Requests the backend to email a CSV export of the session with [sessionUuid] to [email]. */
  suspend fun exportSession(email: String, sessionUuid: String) {
    client.get(ApiEndpoints.ExportSessionByUuid) {
      parameter("email", email)
      parameter("uuid", sessionUuid)
    }
  }
}