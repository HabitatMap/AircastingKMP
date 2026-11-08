package pl.llp.aircasting.data.local

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ActiveSessionsRepositoryTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val repo = LocalActiveSessionsRepository(db.sessionQueries)

  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)

  private fun session(
    uuid: String,
    type: SessionType = SessionType.MOBILE,
    status: SessionStatus = SessionStatus.RECORDING,
    start: Instant = t0,
  ) = Session(
    uuid = uuid,
    type = type,
    status = status,
    name = "Living room",
    tags = listOf("Home"),
    startTime = start,
    timeZone = "Europe/Warsaw",
    measurementInterval = 1.seconds,
    deviceModel = "AirBeamMini",
  )

  @Test
  fun `only unfinished mobile sessions are active, newest first`() = runTest {
    sessions.insert(session("old", start = t0))
    sessions.insert(session("new", start = t0 + 60.seconds))
    sessions.insert(session("lost", status = SessionStatus.DISCONNECTED, start = t0 + 30.seconds))
    sessions.insert(session("fixed", type = SessionType.FIXED))
    sessions.insert(session("done"))
    sessions.finish("done", t0 + 90.seconds)

    assertEquals(listOf("new", "lost", "old"), repo.observeMobile().first().map { it.session.uuid })
  }

  @Test
  fun `each stream carries its newest reading, or none yet`() = runTest {
    sessions.insert(session("s1"))
    val pm1 = streams.findOrCreate("s1", "AirBeamMini-PM1", "µg/m³")
    streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")
    measurements.insertAll(listOf(Measurement(pm1, t0, 3.0), Measurement(pm1, t0 + 1.seconds, 4.0)))

    assertEquals(
      listOf(
        LatestReading("AirBeamMini-PM1", "µg/m³", 4.0, t0 + 1.seconds),
        LatestReading("AirBeamMini-PM2.5", "µg/m³"),
      ),
      repo.observeMobile().first().single().readings,
    )
  }

  @Test
  fun `a new measurement re-emits with the newest reading`() = runTest {
    sessions.insert(session("s1"))
    val pm25 = streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")

    repo.observeMobile().test {
      assertNull(awaitItem().single().readings.single().value)
      measurements.insertAll(listOf(Measurement(pm25, t0, 5.0)))
      assertEquals(5.0, awaitItem().single().readings.single().value)
    }
  }

  @Test
  fun `a finished session drops out`() = runTest {
    sessions.insert(session("s1"))

    repo.observeMobile().test {
      assertEquals(1, awaitItem().size)
      sessions.finish("s1", t0 + 60.seconds)
      assertEquals(emptyList(), awaitItem())
    }
  }
}
