package pl.llp.aircasting.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerProbeTest {

  @Test
  fun `a 401 counts as reachable`() = runTest {
    // We have no credentials for a server we have never talked to. A 401 is the *success*
    // signal: it proves /api/user.json exists, i.e. this really is an AirCasting backend.
    val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }

    assertTrue(HttpServerProbe(engine).reachable("https://my.server"))
  }

  @Test
  fun `a 404 does not count - some other web server answered`() = runTest {
    val engine = MockEngine { respondError(HttpStatusCode.NotFound) }

    assertFalse(HttpServerProbe(engine).reachable("https://not-aircasting.example"))
  }

  @Test
  fun `a transport failure is a failed probe, not a crash`() = runTest {
    // Wrong host, no DNS, no route: the wizard must land on "Connection failed", and an
    // exception escaping here would take the whole ViewModel scope down with it.
    val engine = MockEngine { throw kotlinx.io.IOException("unreachable") }

    assertFalse(HttpServerProbe(engine).reachable("https://nope.invalid"))
  }

  @Test
  fun `the probe targets the candidate server, not the one in use`() = runTest {
    // The whole point: this request must NOT go through the app's configured base URL.
    val engine = MockEngine { respond("{}", HttpStatusCode.OK) }

    HttpServerProbe(engine).reachable("https://candidate.example:8080")

    assertEquals(
      "https://candidate.example:8080/api/user.json",
      engine.requestHistory.single().url.toString(),
    )
  }
}