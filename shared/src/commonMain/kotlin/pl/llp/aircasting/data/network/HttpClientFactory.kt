package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
const val DefaultBackendUrl = "https://aircasting.org"
/**
 * AirCasting requires HTTPS for all traffic to prevent 301 redirects from dropping POST bodies.
 * Strips trailing slashes, enforces the https:// protocol, and preserves custom ports.
 */
fun normalizeBackendUrl(url: String): String {
  val trimmed = url.trim().removeSuffix("/")
  return when {
    trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring(7)
    trimmed.startsWith("https://", ignoreCase = true) -> trimmed
    else -> "https://$trimmed"
  }
}
/**
 * Builds the AirCasting HttpClient over a given engine. Tests inject MockEngine;
 * DI injects the platform engine (OkHttp on Android, Darwin on iOS).
 */
fun createAircastingHttpClient(
  engine: HttpClientEngine,
  baseUrl: () -> String = { DefaultBackendUrl },
): HttpClient = HttpClient(engine) {
  expectSuccess = true // 401/5xx throw instead of surfacing as JSON-decode errors
  install(ContentNegotiation) { json(AircastingJson) }
  install(DefaultRequest) { url(normalizeBackendUrl(baseUrl())) }
}