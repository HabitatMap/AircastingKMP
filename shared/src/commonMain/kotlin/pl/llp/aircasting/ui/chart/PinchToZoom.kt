package pl.llp.aircasting.ui.chart

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import kotlinx.coroutines.launch

/**
 * Two-finger pinch zoom that takes over from Vico's own detector.
 *
 * Vico's detector starts only once `|1 - zoom| × finger spread` passes the touch slop, and it gives
 * up as soon as the chart's pan consumes a move. The chart's touch slop is raised to [PanSlop] so
 * the tooltip can scrub, so with fingers close together a pinch-in never reached that slop before
 * the pan did: zooming out failed. Here a second finger alone means "zoom", no slop.
 *
 * Runs in the Initial pass (parent before children) and consumes the two-finger events, so Vico's
 * pan and zoom both stand down for the rest of the gesture. Zooms around the chart's centre:
 * Vico's public API has no centroid zoom.
 */
@Composable
internal fun Modifier.pinchToZoom(zoomState: VicoZoomState): Modifier {
  val scope = rememberCoroutineScope()
  return pointerInput(zoomState) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
      do {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.count { it.pressed } >= 2) {
          val factor = event.calculateZoom()
          // Read the value when the zoom runs, not now, so back-to-back events compound.
          if (factor != 1f) scope.launch { zoomState.zoom(Zoom { _, _, _ -> zoomState.value * factor }) }
          event.changes.forEach { it.consume() }
        }
      } while (event.changes.any { it.pressed })
    }
  }
}
