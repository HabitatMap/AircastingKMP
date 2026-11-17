package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import pl.llp.aircasting.explore.apiKeyProvided
import pl.llp.aircasting.explore.mapStyle
import platform.CoreGraphics.CGRectMake
import platform.darwin.NSObject
import swiftPMImport.Aircasting.shared.GMSCameraPosition
import swiftPMImport.Aircasting.shared.GMSMapStyle
import swiftPMImport.Aircasting.shared.GMSMapView
import swiftPMImport.Aircasting.shared.GMSMapViewDelegateProtocol

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformMap(
  camera: MapCamera,
  modifier: Modifier,
  overlay: @Composable (camera: () -> MapCamera) -> Unit,
) {
  val delegate = remember { PlatformMapDelegate(camera) }
  val style = mapStyle()
  Box(modifier) {
    UIKitView(
      factory = {
        apiKeyProvided
        GMSMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), camera = camera.toGms()).apply {
          this.delegate = delegate
          settings.rotateGestures = false
          settings.tiltGestures = false
        }
      },
      modifier = Modifier.matchParentSize(),
      update = { map ->
        delegate.style(map, style)
        delegate.moveTo(map, camera)
      },
      properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative),
    )
    overlay { delegate.camera }
  }
}

/** GMSMapView has no Compose state: the delegate mirrors its camera into snapshot state, every frame. */
@OptIn(ExperimentalForeignApi::class)
private class PlatformMapDelegate(initial: MapCamera) : NSObject(), GMSMapViewDelegateProtocol {
  var camera by mutableStateOf(initial)
  private var target = initial
  private var styledWith: String? = null

  /** Fires on every frame of a gesture or animation, and after setCamera. */
  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, didChangeCameraPosition: GMSCameraPosition) {
    val zoom = didChangeCameraPosition.zoom
    camera = didChangeCameraPosition.target.useContents { MapCamera(latitude, longitude, zoom) }
  }
  /** update{} re-runs on every recomposition: only a new target moves the map. */
  fun moveTo(map: GMSMapView, next: MapCamera) {
    if (next == target) return
    target = next
    map.setCamera(next.toGms())
  }

  fun style(map: GMSMapView, json: String) {
    if (json == styledWith) return
    styledWith = json
    map.mapStyle = GMSMapStyle.styleWithJSONString(json, error = null)
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun MapCamera.toGms() = GMSCameraPosition.cameraWithLatitude(centerLatitude, centerLongitude, zoom)
