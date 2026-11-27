package pl.llp.aircasting.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import pl.llp.aircasting.i18n.LocalStrings

/** Decodes the JPEG [load] returns, off the main thread, once per [key]. Null while loading or if there is none. */
@Composable
internal fun rememberJpeg(key: Any?, load: suspend () -> ByteArray?): ImageBitmap? {
  val bitmap by produceState<ImageBitmap?>(null, key) {
    value = withContext(Dispatchers.Default) {
      load()?.let { jpeg ->
        runCatching { jpeg.decodeToImageBitmap() }
          .onFailure { Logger.withTag("NotePhoto").w(it) { "Undecodable note photo" } }
          .getOrNull()
      }
    }
  }
  return bitmap
}

/** Cropped to fill, rounded like the sheet's fields. */
@Composable
internal fun NotePhotoImage(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
  Image(
    bitmap,
    contentDescription = LocalStrings.current.notesPhoto,
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
    contentScale = ContentScale.Crop,
  )
}
