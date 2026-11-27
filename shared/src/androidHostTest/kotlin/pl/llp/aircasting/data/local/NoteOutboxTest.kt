package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.NoteChange
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NoteOutboxTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val notes = LocalNoteRepository(db.noteQueries)
  private val outbox = LocalNoteOutbox(db.noteQueries)

  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)

  private suspend fun sessionWithReading(uuid: String) {
    sessions.insert(
      Session(
        uuid = uuid,
        type = SessionType.MOBILE,
        status = SessionStatus.RECORDING,
        name = "Walk",
        tags = emptyList(),
        startTime = t0,
        timeZone = "Europe/Warsaw",
        measurementInterval = 1.seconds,
      )
    )
    val stream = streams.findOrCreate(uuid, "AirBeam-PM2.5", "µg/m³")
    measurements.insertRecorded(listOf(Measurement(stream, t0 + 1.seconds, 10.0, GeoLocation(50.06, 19.94))))
  }

  /** Added on the phone = upload work, like a recorded measurement in pending_uploads. */
  @Test
  fun `a new note is pending and its session has upload work`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!

    assertEquals(listOf(note), outbox.pending("s1"))
    assertEquals(listOf("s1"), outbox.observeSessionsWithPending().first())
  }

  @Test
  fun `the outbox hands out the photo of a pending note for the create`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke", photo = byteArrayOf(1, 2, 3))!!

    assertContentEquals(byteArrayOf(1, 2, 3), outbox.photo(note.id))
  }

  @Test
  fun `an uploaded note is no longer pending`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!

    outbox.markUploaded(note.id, remoteId = 17, sentText = note.text)

    assertTrue(outbox.pending("s1").isEmpty())
    assertTrue(outbox.observeSessionsWithPending().first().isEmpty())
    assertEquals(listOf(note), notes.observe("s1").first())
  }

  @Test
  fun `pending is per session and oldest first`() = runTest {
    sessionWithReading("s1")
    sessionWithReading("s2")
    val first = notes.add("s1", "first")!!
    val second = notes.add("s1", "second")!!
    notes.add("s2", "other")

    assertEquals(listOf(first, second), outbox.pending("s1"))
    assertEquals(listOf("s1", "s2"), outbox.observeSessionsWithPending().first().sorted())
  }

  @Test
  fun `editing an uploaded note makes a pending edit until it is sent`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!
    outbox.markUploaded(note.id, remoteId = 17, sentText = note.text)

    notes.edit(note.id, "Empty street")

    assertEquals(listOf(NoteChange(note.id, 17, "Empty street")), outbox.pendingEdits("s1"))
    assertEquals(listOf("s1"), outbox.observeSessionsWithPending().first())
    outbox.markEditUploaded(note.id, sentText = "Empty street")
    assertTrue(outbox.pendingEdits("s1").isEmpty())
    assertTrue(outbox.observeSessionsWithPending().first().isEmpty())
  }

  /** Not on the server yet: the create carries the new text, nothing to PATCH. */
  @Test
  fun `editing a note before its upload just changes what the create sends`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!

    notes.edit(note.id, "Empty street")
    outbox.markUploaded(note.id, remoteId = 17, sentText = "Empty street")

    assertTrue(outbox.pendingEdits("s1").isEmpty())
  }

  /** The POST carried the old text; the server needs the new one too. */
  @Test
  fun `an edit made while the create was in flight is still sent`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!

    notes.edit(note.id, "Empty street")
    outbox.markUploaded(note.id, remoteId = 17, sentText = "Smells like smoke")

    assertEquals(listOf(NoteChange(note.id, 17, "Empty street")), outbox.pendingEdits("s1"))
  }

  /** A newer edit that lands while the PATCH is in flight stays pending. */
  @Test
  fun `an edit made while a PATCH was in flight stays pending`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!
    outbox.markUploaded(note.id, remoteId = 17, sentText = note.text)
    notes.edit(note.id, "Empty street")

    notes.edit(note.id, "Empty street at noon")
    outbox.markEditUploaded(note.id, sentText = "Empty street")

    assertEquals(listOf(NoteChange(note.id, 17, "Empty street at noon")), outbox.pendingEdits("s1"))
  }

  @Test
  fun `deleting an uploaded note is a pending delete until purged`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!
    outbox.markUploaded(note.id, remoteId = 17, sentText = note.text)

    notes.delete(note.id)

    assertEquals(listOf(NoteChange(note.id, 17, note.text)), outbox.pendingDeletes("s1"))
    assertTrue(outbox.pendingEdits("s1").isEmpty())
    assertEquals(listOf("s1"), outbox.observeSessionsWithPending().first())
    outbox.purge(note.id)
    assertTrue(outbox.pendingDeletes("s1").isEmpty())
    assertTrue(outbox.observeSessionsWithPending().first().isEmpty())
  }

  /** Never created on the server: no POST, and the uploader drops it without a request. */
  @Test
  fun `deleting a note before its upload cancels the create`() = runTest {
    sessionWithReading("s1")
    val note = notes.add("s1", "Smells like smoke")!!

    notes.delete(note.id)

    assertTrue(outbox.pending("s1").isEmpty())
    assertEquals(listOf(NoteChange(note.id, null, note.text)), outbox.pendingDeletes("s1"))
  }
}
