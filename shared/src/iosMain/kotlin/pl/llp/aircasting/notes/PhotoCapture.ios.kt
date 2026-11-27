package pl.llp.aircasting.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min

/**
 * UIImagePickerController: the camera (needs NSCameraUsageDescription in Info.plist), or the photo
 * library on the simulator, which has no camera. The library picker runs out of process: no permission.
 */
@Composable
actual fun rememberPhotoCapture(onPhoto: (jpeg: ByteArray) -> Unit): PhotoCapture {
  val latest by rememberUpdatedState(onPhoto)
  // The picker holds its delegate weakly: remembering it here keeps it alive while the picker is up.
  val delegate = remember { PickerDelegate { latest(it) } }
  return remember(delegate) { PhotoCapture { delegate.present() } }
}

private class PickerDelegate(private val onPhoto: (ByteArray) -> Unit) :
  NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

  fun present() {
    val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    val picker = UIImagePickerController()
    picker.sourceType = if (UIImagePickerController.isSourceTypeAvailable(camera)) camera
    else UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
    picker.delegate = this
    topViewController()?.presentViewController(picker, animated = true, completion = null)
  }

  override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
    picker.dismissViewControllerAnimated(true, completion = null)
    image?.let(::downscaledJpeg)?.let(onPhoto)
  }

  override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
    picker.dismissViewControllerAnimated(true, completion = null)
  }
}

/** Compose's own view controller may itself be presenting something (a sheet): present above the top one. */
private fun topViewController(): UIViewController? {
  var top = UIApplication.sharedApplication.keyWindow?.rootViewController
  while (top?.presentedViewController != null) top = top.presentedViewController
  return top
}

/** Redrawn at most [NotePhotoMaxEdgePx] wide or tall. Drawing applies imageOrientation, so it comes out upright. */
@OptIn(ExperimentalForeignApi::class)
private fun downscaledJpeg(image: UIImage): ByteArray? {
  val (width, height) = image.size.useContents { width to height }
  if (width <= 0.0 || height <= 0.0) return null
  val scale = min(1.0, NotePhotoMaxEdgePx / max(width, height))
  // Scale 1: pixels, not points — the default format would multiply by the screen's 3x.
  val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0) }
  val scaled = UIGraphicsImageRenderer(CGSizeMake(width * scale, height * scale), format).imageWithActions {
    image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
  }
  return UIImageJPEGRepresentation(scaled, NotePhotoJpegQuality / 100.0)?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
  val bytes = ByteArray(length.toInt())
  if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
  return bytes
}
