package pl.llp.aircasting.data.local

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.mydata.StreamSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PastSessionsRepositoryTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val repo = LocalPastSessionsRepository(db.sessionQueries)

  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)

  private fun session(uuid: String, type: SessionType = SessionType.MOBILE, start: Instant = t0) = Session(
    uuid = uuid,
    type = type,
    status = SessionStatus.RECORDING,
    name = "Walk",
    tags = listOf("Home"),
    startTime = start,
    timeZone = "Europe/Warsaw",
    measurementInterval = 1.seconds,
    deviceModel = "AirBeamMini",
  )

  private suspend fun finished(uuid: String, type: SessionType = SessionType.MOBILE, start: Instant = t0) {
    sessions.insert(session(uuid, type, start))
    sessions.finish(uuid, start + 600.seconds)
  }

  @Test
  fun `finished fixed sessions are past too and recording ones are not`() = runTest {
    finished("old", start = t0)
    finished("new", start = t0 + 3600.seconds)
    finished("fixed", type = SessionType.FIXED, start = t0 + 1800.seconds)
    sessions.insert(session("recording"))
    sessions.insert(session("live fixed", type = SessionType.FIXED))

    assertEquals(listOf("new", "fixed", "old"), repo.observe().first().map { it.session.uuid })
  }

  @Test
  fun `a fixed session gets no aggregates from its few stored readings`() = runTest {
    sessions.insert(session("f1", type = SessionType.FIXED))
    val pm25 = streams.findOrCreate("f1", "AirBeam3-PM2.5", "µg/m³")
    measurements.insertAll(listOf(Measurement(pm25, t0, 7.0)))
    sessions.finish("f1", t0 + 60.seconds)

    assertEquals(listOf(StreamSummary("AirBeam3-PM2.5", "µg/m³")), repo.observe().first().single().streams)
  }

  @Test
  fun `each stream carries its whole-session avg, min and max, or nulls when it never got a reading`() = runTest {
    sessions.insert(session("s1"))
    val pm1 = streams.findOrCreate("s1", "AirBeamMini-PM1", "µg/m³")
    streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")
    measurements.insertAll(
      listOf(Measurement(pm1, t0, 2.0), Measurement(pm1, t0 + 1.seconds, 4.0), Measurement(pm1, t0 + 2.seconds, 9.0)),
    )
    sessions.finish("s1", t0 + 60.seconds)

    assertEquals(
      listOf(
        StreamSummary("AirBeamMini-PM1", "µg/m³", avg = 5.0, min = 2.0, max = 9.0),
        StreamSummary("AirBeamMini-PM2.5", "µg/m³"),
      ),
      repo.observe().first().single().streams,
    )
  }

  @Test
  fun `finishing a recording moves it in`() = runTest {
    sessions.insert(session("s1"))

    repo.observe().test {
      assertEquals(emptyList(), awaitItem())
      sessions.finish("s1", t0 + 60.seconds)
      assertEquals(listOf("s1"), awaitItem().map { it.session.uuid })
    }
  }
}
