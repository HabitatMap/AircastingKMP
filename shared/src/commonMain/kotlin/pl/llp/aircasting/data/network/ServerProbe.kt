package pl.llp.aircasting.data.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

fun interface ServerProbe {
  /** Does [baseUrl] answer as an AirCasting server? */
  suspend fun reachable(baseUrl: String): Boolean
}

class HttpServerProbe(private val engine: HttpClientEngine) : ServerProbe {

  private val log = Logger.withTag("ServerProbe")

  override suspend fun reachable(baseUrl: String): Boolean {
    val client = HttpClient(engine) {
      expectSuccess = false
      install(DefaultRequest) { url(baseUrl) }
    }
    return try {
      client.get(ProbeEndpoint).status in Reachable
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      log.w(e) { "probe of $baseUrl failed" }
      false
    } finally {
      client.close()
    }
  }
  private companion object {
    const val ProbeEndpoint = "/api/user.json"
    val Reachable = setOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized)
  }
}