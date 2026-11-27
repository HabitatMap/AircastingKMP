package pl.llp.aircasting.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.i18n.LocalStrings

/** Figma "Notes list" (1246:404): newest first, each card showing [stream]'s reading at the note. */
@Composable
fun SessionNotesList(
  vm: SessionNotesViewModel,
  details: StationDetails,
  stream: StationStream,
  threshold: SensorThreshold?,
  modifier: Modifier = Modifier,
  onOpen: (Note) -> Unit,
) {
  val notes by remember(stream.sensorName) { vm.notesOn(stream.sensorName) }.collectAsStateWithLifecycle()
  if (notes.isEmpty()) {
    Box(modifier, contentAlignment = Alignment.Center) {
      Text(LocalStrings.current.notesEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }
  LazyColumn(modifier, contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    items(notes, key = { it.note.id }) { item ->
      val photo = if (item.note.hasPhoto) rememberJpeg(item.note.id) { vm.photo(item.note.id) } else null
      NoteCard(
        item,
        stream.sensorName,
        stream.unitSymbol,
        level = item.value?.let { threshold?.levelFor(it) },
        timeZone = details.timeZone,
        modifier = Modifier.clickable { onOpen(item.note) },
        photo = photo,
      )
    }
  }
}
