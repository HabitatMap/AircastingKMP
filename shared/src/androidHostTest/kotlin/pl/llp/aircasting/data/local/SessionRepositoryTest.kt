package pl.llp.aircasting.data.local

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionRepositoryTest {

  private val db = testDatabase()
  private val repo = LocalSessionRepository(db.sessionQueries)

  private val session = Session(
    uuid = "11111111-1111-1111-1111-111111111111",
    type = SessionType.MOBILE,
    status = SessionStatus.RECORDING,
    name = "Morning walk",
    tags = listOf("bike", "commute"),
    startTime = Instant.fromEpochMilliseconds(1_700_000_000_000),
    deviceId = "AA:BB:CC:DD:EE:FF",
    deviceModel = "AirBeam3",
    location = GeoLocation(50.06, 19.94),
    timeZone = "Europe/Warsaw",
    measurementInterval = 1.seconds,
  )

  @Test
  fun `insert then find round-trips every field`() = runTest {
    repo.insert(session)
    assertEquals(session, repo.find(session.uuid))
  }

  @Test
  fun `find returns null for an unknown uuid`() = runTest {
    assertNull(repo.find("nope"))
  }

  @Test
  fun `a session without a location round-trips as null`() = runTest {
    val locationless = session.copy(uuid = "no-loc", location = null)
    repo.insert(locationless)
    assertNull(repo.find("no-loc")?.location)
  }

  @Test
  fun `observeAll re-emits when a session is inserted`() = runTest {
    repo.observeAll().test {
      assertEquals(emptyList(), awaitItem())
      repo.insert(session)
      assertEquals(listOf(session), awaitItem())
    }
  }

  @Test
  fun `finish sets status and end time together`() = runTest {
    val end = session.startTime + 60.seconds
    repo.insert(session)
    repo.finish(session.uuid, end)

    val stored = repo.find(session.uuid)!!
    assertEquals(SessionStatus.FINISHED, stored.status)
    assertEquals(end, stored.endTime)
  }

  @Test
  fun `updateStatus leaves end time untouched`() = runTest {
    repo.insert(session)
    repo.updateStatus(session.uuid, SessionStatus.DISCONNECTED)

    val stored = repo.find(session.uuid)!!
    assertEquals(SessionStatus.DISCONNECTED, stored.status)
    assertNull(stored.endTime)
  }
}