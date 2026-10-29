package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Public, translated link lists. No auth. An unsupported [locale] falls back to `en-US` server-side. */
class AppConfigApi(private val client: HttpClient) {
  suspend fun appConfig(locale: String): AppConfigDto =
    client.get(ApiEndpoints.V3AppConfig) { parameter("locale", locale) }.body()
}