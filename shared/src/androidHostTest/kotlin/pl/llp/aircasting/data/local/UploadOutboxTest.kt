package pl.llp.aircasting.data.local

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class UploadOutboxTest {

  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val outbox = LocalUploadOutbox(db.pendingUploadQueries)

  private val t0 = Instant.fromEpochMilliseconds(1_700_000_000_000)

  private suspend fun seedStream(uuid: String = "33333333-3333-3333-3333-333333333333"): Long {
    sessions.insert(
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
    return streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
  }

  /** The recording path: one call, the row is both stored (for the UI) and queued (for upload). */
  @Test
  fun `insertRecorded stores the measurement and enqueues it`() = runTest {
    val stream = seedStream()
    val m = Measurement(stream, t0, 12.0)

    assertEquals(1, measurements.insertRecorded(listOf(m)))

    assertEquals(listOf(m), measurements.observeRecent(stream, 10).first())
    assertEquals(listOf(m), outbox.pending("33333333-3333-3333-3333-333333333333", 10))
  }

  /**
   * The safety property the whole design exists for: downloaded / explored / fixed data
   * goes through insertAll and must never become upload work.
   */
  @Test
  fun `insertAll stores but never enqueues`() = runTest {
    val stream = seedStream()

    measurements.insertAll(listOf(Measurement(stream, t0, 12.0)))

    assertTrue(outbox.pending("33333333-3333-3333-3333-333333333333", 10).isEmpty())
  }

  /**
   * A duplicate skipped by OR IGNORE was enqueued when first recorded. Re-enqueuing it
   * after an ack would resend a measurement the server already has.
   */
  @Test
  fun `insertRecorded does not re-enqueue a duplicate that was already acknowledged`() = runTest {
    val stream = seedStream()
    val m = Measurement(stream, t0, 12.0)
    measurements.insertRecorded(listOf(m))
    outbox.acknowledge(listOf(m))

    assertEquals(0, measurements.insertRecorded(listOf(m)))
    assertTrue(outbox.pending("33333333-3333-3333-3333-333333333333", 10).isEmpty())
  }

  /** Oldest first, regardless of arrival order — backfill from a BLE sync arrives late. */
  @Test
  fun `pending yields oldest first and respects the limit`() = runTest {
    val stream = seedStream()
    measurements.insertRecorded(
      listOf(
        Measurement(stream, t0 + 2.seconds, 3.0),
        Measurement(stream, t0, 1.0),
        Measurement(stream, t0 + 1.seconds, 2.0),
      )
    )

    val pending = outbox.pending("33333333-3333-3333-3333-333333333333", limit = 2)

    assertEquals(listOf(t0, t0 + 1.seconds), pending.map { it.time })
  }

  /** Each upload is per session uuid (`POST /mobile_sessions/{uuid}/measurements`). */
  @Test
  fun `pending is scoped to one session`() = runTest {
    val a = seedStream("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    val b = seedStream("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    measurements.insertRecorded(listOf(Measurement(a, t0, 1.0), Measurement(b, t0, 2.0)))

    val pending = outbox.pending("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", 10)

    assertEquals(listOf(a), pending.map { it.streamId })
  }

  /** Ack is a DELETE on the outbox only. The data row is never touched. */
  @Test
  fun `acknowledge empties the outbox but keeps the measurement`() = runTest {
    val stream = seedStream()
    val m = Measurement(stream, t0, 12.0)
    measurements.insertRecorded(listOf(m))

    outbox.acknowledge(listOf(m))

    assertTrue(outbox.pending("33333333-3333-3333-3333-333333333333", 10).isEmpty())
    assertEquals(listOf(m), measurements.observeRecent(stream, 10).first())
  }

  /**
   * Atomicity: the second row points at a stream that doesn't exist, so its FK fails.
   * The whole transaction must roll back — including the first row's measurement AND its
   * outbox entry. No half-stored batch, no orphan outbox row.
   */
  @Test
  fun `a failing batch stores and enqueues nothing`() = runTest {
    val stream = seedStream()

    assertFails {
      measurements.insertRecorded(
        listOf(Measurement(stream, t0, 1.0), Measurement(streamId = 999, time = t0, value = 2.0))
      )
    }

    assertTrue(measurements.observeRecent(stream, 10).first().isEmpty())
    assertTrue(outbox.pending("33333333-3333-3333-3333-333333333333", 10).isEmpty())
  }

  /**
   * Proves the composite FK's ON DELETE CASCADE. Without it, deleting the session would
   * cascade into `measurements`, hit an outbox row still referencing one, and *throw* —
   * foreign keys are enforced. So "delete succeeds" is itself the assertion.
   */
  @Test
  fun `deleting a session with pending uploads succeeds and clears them`() = runTest {
    val stream = seedStream()
    measurements.insertRecorded(listOf(Measurement(stream, t0, 12.0)))

    sessions.delete("33333333-3333-3333-3333-333333333333")

    assertTrue(outbox.pending("33333333-3333-3333-3333-333333333333", 10).isEmpty())
  }

  /** The loop's "batchable work" signal: re-emits as rows are recorded and acknowledged. */
  @Test
  fun `recording lists the session once acknowledging the last row clears it`() = runTest {
    val stream = seedStream()
    val rows = listOf(Measurement(stream, t0, 12.0), Measurement(stream, t0 + 1.seconds, 13.0))

    outbox.observeSessionsWithPending().test {
      assertEquals(emptyList(), awaitItem())
      measurements.insertRecorded(rows)
      assertEquals(listOf("33333333-3333-3333-3333-333333333333"), awaitItem())
      outbox.acknowledge(rows)
      assertEquals(emptyList(), awaitItem())
    }
  }

  /** Outbox rule 1: fixed data is the AirBeam's to upload — never feed it to the mobile uploader. */
  @Test
  fun `rows of a fixed session are never listed`() = runTest {
    sessions.insert(
      Session(
        uuid = "fixed",
        type = SessionType.FIXED,
        status = SessionStatus.RECORDING,
        name = "Test",
        tags = emptyList(),
        startTime = t0,
        timeZone = "UTC",
        measurementInterval = 1.seconds,
      )
    )
    val stream = streams.findOrCreate("fixed", "AirBeam-PM2.5", "µg/m³")
    measurements.insertRecorded(listOf(Measurement(stream, t0, 12.0)))

    assertEquals(emptyList(), outbox.observeSessionsWithPending().first())
  }
}
