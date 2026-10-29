package pl.llp.aircasting.settings.help

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.data.network.AppConfigApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkHelpRepositoryTest {

  @Test
  fun requests_app_config_publicly_with_the_locale() = runTest {
    var request: HttpRequestData? = null
    repository { request = it }.links("fr")

    assertEquals("/api/v3/app_config", request!!.url.encodedPath)
    assertEquals("fr", request!!.url.parameters["locale"])
    // `security: []` in the spec — a token here would leak it to a public, cacheable endpoint.
    assertNull(request!!.headers[HttpHeaders.Authorization])
  }

  @Test
  fun maps_both_lists_in_server_order() = runTest {
    val links = repository().links("en-US")

    assertEquals(
      listOf(
        HelpLink("FAQ", "https://www.habitatmap.org/airbeam/faq"),
        HelpLink("How Recording Works", "https://www.habitatmap.org/airbeam/how-it-works"),
      ),
      links.help,
    )
    assertEquals(listOf(HelpLink("Air Quality 101", "https://aircastingactions.org/air-quality-101/")), links.actions)
  }

  private fun repository(onRequest: (HttpRequestData) -> Unit = {}): HelpRepository {
    val engine = MockEngine { request ->
      onRequest(request)
      respond(APP_CONFIG_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return NetworkHelpRepository(AppConfigApi(createAircastingHttpClient(engine)))
  }
}

private const val APP_CONFIG_JSON = """
{"locale":"en-US","default_locale":"en-US",
 "help":[{"title":"FAQ","url":"https://www.habitatmap.org/airbeam/faq"},
         {"title":"How Recording Works","url":"https://www.habitatmap.org/airbeam/how-it-works"}],
 "aircasting_actions":[{"title":"Air Quality 101","url":"https://aircastingactions.org/air-quality-101/"}],
 "learn_more":[{"post_slug":"x","title":"X","category":null,"url":"https://www.habitatmap.org/blog/x","thumbnail_url":null}]}
"""