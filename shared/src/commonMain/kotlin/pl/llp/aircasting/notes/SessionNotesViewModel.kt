package pl.llp.aircasting.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.domain.session.NoteRepository

/** Which note sheet is open on the session details screen. */
sealed interface NoteSheet {
  data object Closed : NoteSheet
  /** [saved]: stored, the sheet should animate out and then call dismiss. [photo]: taken, not stored yet. */
  data class Adding(val error: NoteError? = null, val saved: Boolean = false, val photo: NotePhoto? = null) : NoteSheet
  /** Figma "Edit notes" (1140:29142): live sessions only. [saved] also after a delete. */
  data class Editing(val note: Note, val error: NoteError? = null, val saved: Boolean = false) : NoteSheet
}

enum class NoteError { EmptyText, NoLocation }

/** A JPEG in UI state: compared by content, so state equality (and tests) work on the bytes. */
class NotePhoto(val jpeg: ByteArray) {
  override fun equals(other: Any?) = other is NotePhoto && jpeg.contentEquals(other.jpeg)
  override fun hashCode() = jpeg.contentHashCode()
}

/** Notes of one of our own mobile sessions: the lists, and the add / edit sheet's state. */
class SessionNotesViewModel(
  private val sessionUuid: String,
  private val repository: NoteRepository,
) : ViewModel() {

  private val onStream = mutableMapOf<String, StateFlow<List<NoteOnStream>>>()

  /** The notes as seen on [sensorName] (the selected chip). One cached flow per stream. */
  fun notesOn(sensorName: String): StateFlow<List<NoteOnStream>> = onStream.getOrPut(sensorName) {
    repository.observeOnStream(sessionUuid, sensorName)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  }

  // In the ViewModel, not rememberSaveable: a save in flight must find the sheet it belongs to.
  private val _sheet = MutableStateFlow<NoteSheet>(NoteSheet.Closed)
  val sheet: StateFlow<NoteSheet> = _sheet.asStateFlow()

  fun openAdd() {
    _sheet.value = NoteSheet.Adding()
  }

  /** A photo from the camera for the note being added; a retake replaces it. */
  fun attachPhoto(jpeg: ByteArray) {
    _sheet.update { if (it is NoteSheet.Adding) it.copy(photo = NotePhoto(jpeg), error = null) else it }
  }

  /** The stored photo of [noteId], for its card or the edit sheet. */
  suspend fun photo(noteId: Long): ByteArray? = repository.photo(noteId)

  fun openEdit(note: Note) {
    _sheet.value = NoteSheet.Editing(note)
  }

  /** Done or Save on whichever sheet is open. */
  fun save(text: String) {
    val trimmed = text.trim()
    when (val sheet = _sheet.value) {
      NoteSheet.Closed -> Unit
      is NoteSheet.Adding ->
        if (trimmed.isEmpty()) _sheet.value = sheet.copy(error = NoteError.EmptyText)
        else viewModelScope.launch {
          val note = repository.add(sessionUuid, trimmed, sheet.photo?.jpeg)
          _sheet.value = if (note == null) sheet.copy(error = NoteError.NoLocation) else sheet.copy(error = null, saved = true)
        }
      is NoteSheet.Editing ->
        if (trimmed.isEmpty()) _sheet.value = sheet.copy(error = NoteError.EmptyText)
        else viewModelScope.launch {
          repository.edit(sheet.note.id, trimmed)
          _sheet.value = NoteSheet.Editing(sheet.note, saved = true)
        }
    }
  }

  fun delete() {
    val sheet = _sheet.value as? NoteSheet.Editing ?: return
    viewModelScope.launch {
      repository.delete(sheet.note.id)
      _sheet.value = NoteSheet.Editing(sheet.note, saved = true)
    }
  }

  fun dismiss() {
    _sheet.value = NoteSheet.Closed
  }
}
