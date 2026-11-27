package pl.llp.aircasting.navigation

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add_notes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.active.SessionGraph
import pl.llp.aircasting.active.SessionMap
import pl.llp.aircasting.di.SessionDetailsQualifier
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.explore.StationDetailsScreen
import pl.llp.aircasting.explore.StationDetailsState
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.notes.NoteEditorSheet
import pl.llp.aircasting.notes.SessionNotesList
import pl.llp.aircasting.notes.noteMarkers
import pl.llp.aircasting.notes.rememberPhotoCapture
import pl.llp.aircasting.notes.SessionNotesViewModel
import pl.llp.aircasting.explore.StationDetailsViewModel
import pl.llp.aircasting.map.StationMap

/**
 * One of our own sessions, read live from the local DB. [sensorName] = the chip selected on the
 * Active card, so details open on the same stream; null when the card had no streams yet.
 */
@Serializable
data class SessionDetailsRoute(val uuid: String, val sensorName: String? = null)

@Composable
fun SessionDetailsDestination(route: SessionDetailsRoute, onBack: () -> Unit) {
  val vm = koinViewModel<StationDetailsViewModel>(qualifier = SessionDetailsQualifier) {
    parametersOf(route.uuid, route.sensorName)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  val notes = koinViewModel<SessionNotesViewModel> { parametersOf(route.uuid) }
  val noteSheet by notes.sheet.collectAsStateWithLifecycle()
  val details = (state as? StationDetailsState.Content)?.details
  StationDetailsScreen(
    state, onBack = onBack, onRetry = vm::load, onSelectStream = vm::selectStream,
    onThresholdChange = vm::saveThreshold,
    graph = { session, stream, threshold, modifier ->
      val onStream by remember(stream.sensorName) { notes.notesOn(stream.sensorName) }.collectAsStateWithLifecycle()
      val markers = remember(onStream, session.timeZone) { noteMarkers(onStream, session.timeZone) }
      SessionGraph(session, stream, threshold, modifier, notes = markers)
    },
    map = { details, stream, threshold, fullScreen, modifier ->
      if (details.type == SessionType.FIXED) StationMap(details, stream, threshold, interactive = fullScreen, modifier)
      else {
        // Notes sit where they were written, whatever the stream: any chip's flow has them all.
        val onStream by remember(stream.sensorName) { notes.notesOn(stream.sensorName) }.collectAsStateWithLifecycle()
        SessionMap(details, stream, threshold, modifier, notes = remember(onStream) { onStream.map { it.note.location } })
      }
    },
    notes = if (details?.type == SessionType.MOBILE) {
      { session, stream, threshold, modifier ->
        // Edit and delete on finished sessions too, as legacy did; only adding needs a live session.
        SessionNotesList(notes, session, stream, threshold, modifier, onOpen = notes::openEdit)
      }
    } else null,
    actions = {
      // Notes exist only on mobile sessions (/mobile_sessions/{uuid}/notes); adding only while live.
      if (details?.type == SessionType.MOBILE && details.endTime == null) {
        IconButton(onClick = notes::openAdd) {
          Icon(painterResource(Res.drawable.ic_add_notes), contentDescription = LocalStrings.current.notesAdd)
        }
      }
    },
  )
  val photoCapture = rememberPhotoCapture(onPhoto = notes::attachPhoto)
  NoteEditorSheet(
    noteSheet, onSave = notes::save, onDelete = notes::delete, onDismiss = notes::dismiss,
    onTakePhoto = photoCapture::launch, loadPhoto = notes::photo,
  )
}