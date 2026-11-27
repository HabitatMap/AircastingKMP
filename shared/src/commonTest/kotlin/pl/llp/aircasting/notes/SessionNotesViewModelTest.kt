package pl.llp.aircasting.notes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.domain.session.NoteRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SessionNotesViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val repo = FakeNoteRepository()

  @Test
  fun `starts with the sheet closed`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)

    assertEquals(NoteSheet.Closed, vm.sheet.value)
  }

  @Test
  fun `add opens an empty add sheet`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)

    vm.openAdd()

    assertEquals(NoteSheet.Adding(), vm.sheet.value)
  }

  /** Legacy: "Notes description cannot be empty". Nothing is stored and the sheet stays open. */
  @Test
  fun `saving blank text shows the empty error and stores nothing`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()

    vm.save("   ")
    advanceUntilIdle()

    assertEquals(NoteSheet.Adding(error = NoteError.EmptyText), vm.sheet.value)
    assertTrue(repo.added.isEmpty())
  }

  @Test
  fun `saving stores the trimmed text and lets the sheet close`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()

    vm.save("  Smells like smoke \n")
    advanceUntilIdle()

    assertEquals(listOf("s1" to "Smells like smoke"), repo.added)
    assertEquals(NoteSheet.Adding(saved = true), vm.sheet.value)
  }

  @Test
  fun `a taken photo goes into the add sheet and is saved with the note`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()

    vm.attachPhoto(byteArrayOf(1, 2, 3))
    assertEquals(NoteSheet.Adding(photo = NotePhoto(byteArrayOf(1, 2, 3))), vm.sheet.value)
    vm.save("Smells like smoke")
    advanceUntilIdle()

    assertContentEquals(byteArrayOf(1, 2, 3), repo.addedPhotos.single())
  }

  /** The user fixes the text; the photo they took must still be there. */
  @Test
  fun `an empty text error keeps the photo`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()
    vm.attachPhoto(byteArrayOf(1, 2, 3))

    vm.save(" ")

    assertEquals(NoteSheet.Adding(error = NoteError.EmptyText, photo = NotePhoto(byteArrayOf(1, 2, 3))), vm.sheet.value)
  }

  @Test
  fun `a session without a located reading shows the location error`() = runTest {
    repo.hasLocation = false
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()

    vm.save("Smells like smoke")
    advanceUntilIdle()

    assertEquals(NoteSheet.Adding(error = NoteError.NoLocation), vm.sheet.value)
  }

  @Test
  fun `tapping a note opens it for editing`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)

    vm.openEdit(NOTE)

    assertEquals(NoteSheet.Editing(NOTE), vm.sheet.value)
  }

  @Test
  fun `saving an edit stores the trimmed text and lets the sheet close`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openEdit(NOTE)

    vm.save(" Empty street ")
    advanceUntilIdle()

    assertEquals(listOf(NOTE.id to "Empty street"), repo.edited)
    assertEquals(NoteSheet.Editing(NOTE, saved = true), vm.sheet.value)
  }

  @Test
  fun `an edit cannot empty the note`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openEdit(NOTE)

    vm.save("  ")
    advanceUntilIdle()

    assertEquals(NoteSheet.Editing(NOTE, error = NoteError.EmptyText), vm.sheet.value)
    assertTrue(repo.edited.isEmpty())
  }

  @Test
  fun `delete removes the note and lets the sheet close`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openEdit(NOTE)

    vm.delete()
    advanceUntilIdle()

    assertEquals(listOf(NOTE.id), repo.deleted)
    assertEquals(NoteSheet.Editing(NOTE, saved = true), vm.sheet.value)
  }

  @Test
  fun `dismiss closes the sheet`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    vm.openAdd()

    vm.dismiss()

    assertEquals(NoteSheet.Closed, vm.sheet.value)
  }

  @Test
  fun `notesOn follows the repository for that stream`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)
    val pm25 = vm.notesOn("AirBeam-PM2.5")
    // stateIn(WhileSubscribed): the screen's collector is what keeps the DB flow running.
    backgroundScope.launch { pm25.collect {} }
    advanceUntilIdle()

    repo.notes.value = listOf(NOTE)
    advanceUntilIdle()

    assertEquals(listOf(NoteOnStream(NOTE, value = 8.0)), pm25.value)
    assertEquals(listOf("AirBeam-PM2.5"), repo.streamsObserved)
  }

  /** Switching chips back and forth reuses the stream's flow instead of piling up subscriptions. */
  @Test
  fun `notesOn returns the same flow for the same stream`() = runTest {
    val vm = SessionNotesViewModel("s1", repo)

    assertSame(vm.notesOn("AirBeam-PM2.5"), vm.notesOn("AirBeam-PM2.5"))
  }

  private companion object {
    val NOTE = Note(1, "s1", Instant.fromEpochMilliseconds(1_786_000_000_000), "Smells like smoke", GeoLocation(50.06, 19.94))
  }
}

class FakeNoteRepository : NoteRepository {
  val notes = MutableStateFlow<List<Note>>(emptyList())
  val added = mutableListOf<Pair<String, String>>()
  var hasLocation = true

  val streamsObserved = mutableListOf<String>()

  override fun observe(sessionUuid: String): Flow<List<Note>> = notes.map { all -> all.filter { it.sessionUuid == sessionUuid } }

  /** Every note reads 8.0 on whichever stream: enough to see the value is passed through. */
  override fun observeOnStream(sessionUuid: String, sensorName: String): Flow<List<NoteOnStream>> {
    streamsObserved += sensorName
    return observe(sessionUuid).map { all -> all.map { NoteOnStream(it, value = 8.0) } }
  }

  val edited = mutableListOf<Pair<Long, String>>()
  val deleted = mutableListOf<Long>()

  override suspend fun edit(noteId: Long, text: String) { edited += noteId to text }
  override suspend fun delete(noteId: Long) { deleted += noteId }

  val addedPhotos = mutableListOf<ByteArray?>()

  override suspend fun photo(noteId: Long): ByteArray? = null

  override suspend fun add(sessionUuid: String, text: String, photo: ByteArray?): Note? {
    if (!hasLocation) return null
    added += sessionUuid to text
    addedPhotos += photo
    return Note(added.size.toLong(), sessionUuid, Instant.fromEpochMilliseconds(0), text, GeoLocation(0.0, 0.0))
  }
}
