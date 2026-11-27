package pl.llp.aircasting.notes

import androidx.compose.runtime.Composable

/** Long edge of a note photo as stored and uploaded. The server keeps 600 px; twice that stays sharp here. */
internal const val NotePhotoMaxEdgePx = 1200
internal const val NotePhotoJpegQuality = 80

fun interface PhotoCapture {
  fun launch()
}

/**
 * Takes a photo for a note: the camera, or the photo library where there is none (an emulator, the iOS
 * simulator). [onPhoto] gets an upright JPEG, long edge at most [NotePhotoMaxEdgePx]; it isn't called
 * when the user cancels. Platform code: the camera UI and the image codecs are native on each side.
 */
@Composable
expect fun rememberPhotoCapture(onPhoto: (jpeg: ByteArray) -> Unit): PhotoCapture
