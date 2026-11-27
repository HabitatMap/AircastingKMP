package pl.llp.aircasting.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NoteRepositoryTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val notes = LocalNoteRepository(db.noteQueries)

  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)
  private val here = GeoLocation(50.06, 19.94)
  private val there = GeoLocation(50.07, 19.95)

  private suspend fun session(uuid: String = "s1") = sessions.insert(
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

  private suspend fun reading(stream: Long, second: Int, location: GeoLocation?) =
    measurements.insertRecorded(listOf(Measurement(stream, t0 + second.seconds, 10.0, location)))

  /** Legacy AddNoteBottomSheet: a note sits at the session's last reading — on the graph and the map. */
  @Test
  fun `add stamps the note with the newest located reading of any stream`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    val pm1 = streams.findOrCreate("s1", "AirBeam-PM1", "µg/m³")
    reading(pm25, second = 1, location = here)
    reading(pm1, second = 2, location = there)

    val note = notes.add("s1", "Smells like smoke")

    assertEquals(Note(note!!.id, "s1", t0 + 2.seconds, "Smells like smoke", there), note)
    assertEquals(listOf(note), notes.observe("s1").first())
  }

  /** A reading without a fix has nowhere to put the note on the map, so it is skipped. */
  @Test
  fun `add skips newer readings that have no location`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)
    reading(pm25, second = 2, location = null)

    val note = notes.add("s1", "Smells like smoke")

    assertEquals(t0 + 1.seconds, note?.time)
    assertEquals(here, note?.location)
  }

  /** The API requires a location; legacy refused with NotesNoLocationError. */
  @Test
  fun `add stores nothing and returns null when no reading has a location`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = null)

    assertNull(notes.add("s1", "Smells like smoke"))
    assertTrue(notes.observe("s1").first().isEmpty())
  }

  @Test
  fun `observe lists only the notes of this session newest first`() = runTest {
    session("s1")
    session("s2")
    val s1 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    val s2 = streams.findOrCreate("s2", "AirBeam-PM2.5", "µg/m³")
    reading(s1, second = 1, location = here)
    val first = notes.add("s1", "first")
    reading(s1, second = 2, location = here)
    val second = notes.add("s1", "second")
    reading(s2, second = 3, location = here)
    notes.add("s2", "other session")

    assertEquals(listOf(second, first), notes.observe("s1").first())
  }

  /** The Notes tab card and the graph marker show the selected stream's value where the note sits. */
  @Test
  fun `observeOnStream gives each note the reading of that stream at the note time`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    val pm1 = streams.findOrCreate("s1", "AirBeam-PM1", "µg/m³")
    measurements.insertRecorded(listOf(Measurement(pm25, t0 + 1.seconds, 8.0, here)))
    measurements.insertRecorded(listOf(Measurement(pm1, t0 + 2.seconds, 3.0, here)))
    val note = notes.add("s1", "Smells like smoke")!!
    measurements.insertRecorded(listOf(Measurement(pm25, t0 + 3.seconds, 99.0, here)))

    // PM2.5 had no reading at 2 s: its latest one before the note (1 s), not the later 99.
    assertEquals(listOf(NoteOnStream(note, 8.0)), notes.observeOnStream("s1", "AirBeam-PM2.5").first())
    assertEquals(listOf(NoteOnStream(note, 3.0)), notes.observeOnStream("s1", "AirBeam-PM1").first())
  }

  @Test
  fun `observeOnStream has no value when the stream had no reading by the note time`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    val pm1 = streams.findOrCreate("s1", "AirBeam-PM1", "µg/m³")
    reading(pm25, second = 1, location = here)
    val note = notes.add("s1", "Smells like smoke")!!
    reading(pm1, second = 2, location = here)

    assertEquals(listOf(NoteOnStream(note, null)), notes.observeOnStream("s1", "AirBeam-PM1").first())
  }

  /** The lists only say there is one; the bytes load per card, so a reading a second doesn't reload them. */
  @Test
  fun `a photo is stored with the note and loaded on its own`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)
    val jpeg = byteArrayOf(1, 2, 3)

    val note = notes.add("s1", "Smells like smoke", photo = jpeg)!!

    assertTrue(note.hasPhoto)
    assertEquals(listOf(note), notes.observe("s1").first())
    assertContentEquals(jpeg, notes.photo(note.id))
  }

  @Test
  fun `a note without a photo has none`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)

    val note = notes.add("s1", "Smells like smoke")!!

    assertFalse(note.hasPhoto)
    assertNull(notes.photo(note.id))
  }

  @Test
  fun `edit changes the text and keeps the note in place`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)
    val note = notes.add("s1", "Smells like smoke")!!

    notes.edit(note.id, "Empty street but high pollution")

    assertEquals(listOf(note.copy(text = "Empty street but high pollution")), notes.observe("s1").first())
  }

  /** Gone from the screen at once; the row lingers only until the server hears of it (NoteOutbox). */
  @Test
  fun `delete hides the note from both lists`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)
    val note = notes.add("s1", "Smells like smoke")!!

    notes.delete(note.id)

    assertTrue(notes.observe("s1").first().isEmpty())
    assertTrue(notes.observeOnStream("s1", "AirBeam-PM2.5").first().isEmpty())
  }

  @Test
  fun `deleting the session deletes its notes`() = runTest {
    session()
    val pm25 = streams.findOrCreate("s1", "AirBeam-PM2.5", "µg/m³")
    reading(pm25, second = 1, location = here)
    notes.add("s1", "Smells like smoke")

    sessions.delete("s1")

    assertTrue(notes.observe("s1").first().isEmpty())
  }
}
