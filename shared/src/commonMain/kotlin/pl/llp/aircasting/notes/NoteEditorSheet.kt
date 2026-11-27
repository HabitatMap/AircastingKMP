package pl.llp.aircasting.notes

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add_a_photo
import aircasting.shared.generated.resources.ic_photo_camera
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.settings.SettingsSheet
import pl.llp.aircasting.ui.components.ConfirmDialog
import pl.llp.aircasting.ui.components.OutlinedActionButton
import kotlin.math.min

/**
 * Figma "Bottom sheet - add notes" (1138:28658), "… photo added" (1140:29086) and "Edit notes"
 * (1140:29142). Done and Save both save; editing adds Delete. Once stored ([NoteSheet.Adding.saved] /
 * [NoteSheet.Editing.saved]) the sheet slides out, then [onDismiss]. A photo is taken while adding
 * ([onTakePhoto], again to retake); editing shows the stored one ([loadPhoto]) without changing it.
 */
@Composable
fun NoteEditorSheet(
  sheet: NoteSheet,
  onSave: (text: String) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
  onTakePhoto: () -> Unit,
  loadPhoto: suspend (noteId: Long) -> ByteArray?,
) {
  val (error, saved) = when (sheet) {
    NoteSheet.Closed -> return
    is NoteSheet.Adding -> sheet.error to sheet.saved
    is NoteSheet.Editing -> sheet.error to sheet.saved
  }
  val editing = sheet is NoteSheet.Editing
  val strings = LocalStrings.current
  // Saveable: rotation keeps a half-written note. Keyed by note, so another note opens with its own text.
  var text by rememberSaveable((sheet as? NoteSheet.Editing)?.note?.id) {
    mutableStateOf((sheet as? NoteSheet.Editing)?.note?.text.orEmpty())
  }
  // Deleting can't be undone: ask first. Saveable, so rotation keeps the question open.
  var confirmingDelete by rememberSaveable { mutableStateOf(false) }
  if (confirmingDelete) ConfirmDialog(
    title = strings.notesDeleteConfirmTitle,
    body = strings.notesDeleteConfirmBody,
    confirmLabel = strings.notesDelete,
    dismissLabel = strings.cancel,
    onConfirm = { confirmingDelete = false; onDelete() },
    onDismiss = { confirmingDelete = false },
  )
  SettingsSheet(
    title = if (editing) strings.notesEditTitle else strings.notesTitle,
    onDismiss = onDismiss,
    onConfirm = { onSave(text) },
    closing = saved,
    // Not scrollable: the photo takes what height is left (Figma's 380 dp square doesn't fit a phone).
    scrollable = false,
  ) {
    Column(
      // weight bounds this Column by the sheet's height, so the photo's weight below has a limit to fit.
      Modifier.weight(1f, fill = false).padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // Figma's photo is square and full width. Here it stays full width, in line with the other
      // fields, and takes the height the rest leaves over (weight, not filled when there's more), up to
      // square: on a short sheet it gets wider than tall and the photo is cropped to fill it.
      val photoFrame = Modifier.weight(1f, fill = false).layout { measurable, constraints ->
        val width = constraints.maxWidth
        val height = min(width, constraints.maxHeight)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.place(0, 0) }
      }
      when (sheet) {
        is NoteSheet.Adding -> {
          Text(strings.notesHint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
          val taken = sheet.photo
          val bitmap = taken?.let { rememberJpeg(it) { it.jpeg } }
          if (bitmap == null) AddPhotoSlot(onTakePhoto)
          else Box(photoFrame) {
            NotePhotoImage(bitmap, Modifier.fillMaxSize())
            RetakeButton(onTakePhoto, Modifier.align(Alignment.BottomEnd).padding(12.dp))
          }
        }
        is NoteSheet.Editing -> if (sheet.note.hasPhoto) {
          rememberJpeg(sheet.note.id) { loadPhoto(sheet.note.id) }?.let { NotePhotoImage(it, photoFrame) }
        }
        NoteSheet.Closed -> Unit
      }
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp),
        label = { Text(strings.notesTitle) },
        placeholder = { Text(strings.notesPlaceholder) },
        isError = error != null,
        supportingText = error?.let { { Text(strings.describe(it), color = MaterialTheme.colorScheme.error) } },
      )
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (editing) OutlinedActionButton(
          strings.notesDelete,
          onClick = { confirmingDelete = true },
          Modifier.fillMaxWidth(),
          contentColor = MaterialTheme.colorScheme.error,
          borderColor = MaterialTheme.colorScheme.error,
        )
        OutlinedActionButton(strings.save, onClick = { onSave(text) }, Modifier.fillMaxWidth())
      }
    }
  }
}

/** Figma "Photo attachment" (1141:277): dashed outline, icon in a tinted disc, "Add photo". */
@Composable
private fun AddPhotoSlot(onClick: () -> Unit) {
  val outline = MaterialTheme.colorScheme.outline
  val shape = RoundedCornerShape(12.dp)
  Row(
    Modifier.fillMaxWidth()
      .height(64.dp)
      .clip(shape)
      .clickable(onClick = onClick)
      .drawBehind {
        drawRoundRect(
          outline,
          cornerRadius = CornerRadius(12.dp.toPx()),
          style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
      }
      .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(painterResource(Res.drawable.ic_add_a_photo), contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
    Text(LocalStrings.current.notesAddPhoto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
  }
}

/** Figma 1140:29086: a small camera button on the taken photo. */
@Composable
private fun RetakeButton(onClick: () -> Unit, modifier: Modifier) {
  IconButton(
    onClick,
    modifier.size(40.dp),
    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Icon(painterResource(Res.drawable.ic_photo_camera), contentDescription = LocalStrings.current.notesRetakePhoto, Modifier.size(20.dp))
  }
}

private fun Strings.describe(error: NoteError) = when (error) {
  NoteError.EmptyText -> notesEmptyError
  NoteError.NoLocation -> notesNoLocationError
}
