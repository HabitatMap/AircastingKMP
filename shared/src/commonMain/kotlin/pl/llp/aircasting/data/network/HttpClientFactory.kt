package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json


const val DefaultBackendUrl = "https://aircasting.org"
/**
 * Builds the AirCasting HttpClient over a given engine. Tests inject MockEngine;
 * DI injects the platform engine (OkHttp on Android, Darwin on iOS).
 */
fun createAircastingHttpClient(
  engine: HttpClientEngine,
  baseUrl: () -> String = { DefaultBackendUrl },
): HttpClient = HttpClient(engine) {
  expectSuccess = true   // 401/5xx throw instead of surfacing as a JSON-decode error
  install(ContentNegotiation) { json(AircastingJson) }
  install(DefaultRequest) { url(baseUrl()) }
}