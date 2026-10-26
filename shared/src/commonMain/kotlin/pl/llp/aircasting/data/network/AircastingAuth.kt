package pl.llp.aircasting.data.network


import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

fun HttpRequestBuilder.aircastingBearerAuth(userToken: String) {
  header(HttpHeaders.Authorization, "Bearer $userToken")
}
/**
 * Fixed session measurement upload token: HTTP Bearer with the session_token.
 */
fun HttpRequestBuilder.aircastingSessionTokenAuth(sessionToken: String) {
  header(HttpHeaders.Authorization, "Bearer $sessionToken")
}