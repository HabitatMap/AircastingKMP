package pl.llp.aircasting.notes

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * The system camera app via TakePicture: no CAMERA permission needed, as long as the manifest
 * doesn't declare it. It writes to a FileProvider uri (androidApp manifest, `<package>.notephotos`).
 */
@Composable
actual fun rememberPhotoCapture(onPhoto: (jpeg: ByteArray) -> Unit): PhotoCapture {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val latest by rememberUpdatedState(onPhoto)
  val target = remember(context) { captureUri(context) }
  // Decoding a 12 MP photo takes a while: off the main thread.
  val deliver = { uri: Uri ->
    scope.launch {
      val jpeg = withContext(Dispatchers.Default) {
        runCatching { downscaledJpeg(context, uri) }.onFailure { log.e(it) { "Note photo decode failed" } }.getOrNull()
      }
      jpeg?.let(latest)
    }
  }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(deliver) }
  val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken -> if (taken) deliver(target) }
  return remember(camera, picker, target) {
    PhotoCapture {
      try {
        camera.launch(target)
      } catch (e: ActivityNotFoundException) {
        log.i { "No camera app; picking from the gallery" }
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      }
    }
  }
}

private val log = Logger.withTag("NotePhoto")

/** One reused file: the photo is copied into the DB on save, so only the latest capture matters. */
private fun captureUri(context: Context): Uri {
  val file = File(context.cacheDir, "note_photos/capture.jpg").apply { parentFile?.mkdirs() }
  return FileProvider.getUriForFile(context, "${context.packageName}.notephotos", file)
}

private fun downscaledJpeg(context: Context, uri: Uri): ByteArray? {
  val resolver = context.contentResolver
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  // A bounds-only decode always returns null; the size lands in `bounds`.
  resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
  val longEdge = max(bounds.outWidth, bounds.outHeight)
  if (longEdge <= 0) return null
  // Power-of-two subsampling while decoding: never holds the full-size bitmap in memory.
  var sample = 1
  while (longEdge / (sample * 2) >= NotePhotoMaxEdgePx) sample *= 2
  val decoded = resolver.openInputStream(uri)?.use {
    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
  } ?: return null
  // Camera apps store the sensor's orientation in EXIF instead of rotating the pixels.
  val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees() } ?: 0
  val scale = min(1f, NotePhotoMaxEdgePx.toFloat() / max(decoded.width, decoded.height))
  val matrix = Matrix().apply {
    postScale(scale, scale)
    postRotate(rotation.toFloat())
  }
  val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
  return ByteArrayOutputStream().use {
    upright.compress(Bitmap.CompressFormat.JPEG, NotePhotoJpegQuality, it)
    it.toByteArray()
  }
}

private fun ExifInterface.rotationDegrees(): Int = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
  ExifInterface.ORIENTATION_ROTATE_90 -> 90
  ExifInterface.ORIENTATION_ROTATE_180 -> 180
  ExifInterface.ORIENTATION_ROTATE_270 -> 270
  else -> 0
}
