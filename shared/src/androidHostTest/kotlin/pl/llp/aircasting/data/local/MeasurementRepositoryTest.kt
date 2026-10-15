package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MeasurementRepositoryTest {

  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries)

  private val uuid = "22222222-2222-2222-2222-222222222222"
  private val t0 = Instant.fromEpochMilliseconds(1_700_000_000_000)

  private suspend fun seedSession() = sessions.insert(
    Session(
      uuid = uuid,
      type = SessionType.MOBILE,
      status = SessionStatus.RECORDING,
      name = "Test",
      tags = emptyList(),
      startTime = t0,
      timeZone = "UTC",
      measurementInterval = 1.seconds,
    )
  )

  @Test
  fun `findOrCreate returns the same stream id for the same sensor`() = runTest {
    seedSession()
    val first = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    val second = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    assertEquals(first, second)
    assertEquals(1, streams.findBySession(uuid).size)
  }

  @Test
  fun `insertAll ignores a measurement that already exists for stream and time`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")

    assertEquals(1, measurements.insertAll(listOf(Measurement(stream, t0, value = 12.0))))
    assertEquals(0, measurements.insertAll(listOf(Measurement(stream, t0, value = 99.0))))

    val stored = measurements.observeRecent(stream, limit = 10).first()
    assertEquals(1, stored.size)
    assertEquals(12.0, stored.single().value)
  }

  @Test
  fun `insertAll counts only newly stored rows in a partially overlapping batch`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    measurements.insertAll(listOf(Measurement(stream, t0, 1.0)))

    val stored = measurements.insertAll(
      listOf(
        Measurement(stream, t0, 1.0),
        Measurement(stream, t0 + 1.seconds, 2.0),
        Measurement(stream, t0 + 2.seconds, 3.0),
      )
    )

    assertEquals(2, stored)
  }

  @Test
  fun `two streams may hold the same timestamp`() = runTest {
    seedSession()
    val pm = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    val rh = streams.findOrCreate(uuid, "AirBeam-RH", "%")

    measurements.insertAll(
      listOf(Measurement(pm, t0, 12.0), Measurement(rh, t0, 45.0))
    )

    assertEquals(1, measurements.observeRecent(pm, 10).first().size)
    assertEquals(1, measurements.observeRecent(rh, 10).first().size)
  }

  @Test
  fun `a measurement round-trips its location`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    val here = GeoLocation(50.06, 19.94)

    measurements.insertAll(listOf(Measurement(stream, t0, 12.0, location = here)))

    assertEquals(here, measurements.observeRecent(stream, 10).first().single().location)
  }

  @Test
  fun `lastTime returns the newest timestamp for the stream`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    measurements.insertAll(
      listOf(
        Measurement(stream, t0, 1.0),
        Measurement(stream, t0 + 2.seconds, 2.0),
        Measurement(stream, t0 + 1.seconds, 3.0),
      )
    )
    assertEquals(t0 + 2.seconds, measurements.lastTime(stream))
  }

  @Test
  fun `lastTime is null for an empty stream`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    assertNull(measurements.lastTime(stream))
  }

  @Test
  fun `pendingUpload yields oldest first and markUploaded clears them`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    val batch = listOf(
      Measurement(stream, t0 + 1.seconds, 2.0),
      Measurement(stream, t0, 1.0),
    )
    measurements.insertAll(batch)

    val pending = measurements.pendingUpload(uuid, limit = 10)
    assertEquals(listOf(t0, t0 + 1.seconds), pending.map { it.time })

    measurements.markUploaded(pending)
    assertTrue(measurements.pendingUpload(uuid, limit = 10).isEmpty())
  }

  @Test
  fun `deleting a session removes its streams and measurements`() = runTest {
    seedSession()
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    measurements.insertAll(listOf(Measurement(stream, t0, 12.0)))

    sessions.delete(uuid)

    assertTrue(streams.findBySession(uuid).isEmpty())
    assertTrue(measurements.observeRecent(stream, 10).first().isEmpty())
  }
}