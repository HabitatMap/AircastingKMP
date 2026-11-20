package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.apiKeyProvided
import pl.llp.aircasting.explore.mapStyle
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2D
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
  onTap: (at: GeoLocation, camera: MapCamera) -> Unit,
  onGestureStart: () -> Unit,
  onGestureEnd: (camera: MapCamera) -> Unit,
  overlay: @Composable (camera: () -> MapCamera) -> Unit,
) {
  val currentOnTap by rememberUpdatedState(onTap)
  val currentOnGestureStart by rememberUpdatedState(onGestureStart)
  val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
  val delegate = remember {
    PlatformMapDelegate(
      camera,
      onTap = { at, now -> currentOnTap(at, now) },
      onGestureStart = { currentOnGestureStart() },
      onGestureEnd = { currentOnGestureEnd(it) },
    )
  }

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
private class PlatformMapDelegate(
  initial: MapCamera,
  private val onTap: (GeoLocation, MapCamera) -> Unit,
  private val onGestureStart: () -> Unit,
  private val onGestureEnd: (MapCamera) -> Unit,
) : NSObject(), GMSMapViewDelegateProtocol {
  var camera by mutableStateOf(initial)
  private var target = initial
  private var styledWith: String? = null
  /** Between a gesture's willMove and the idle that ends it (fling included). */
  private var gesturing = false

  /** [willMove] = "by a gesture"; false = our own setCamera: not the user's move. */
  override fun mapView(mapView: GMSMapView, willMove: Boolean) {
    if (!willMove || gesturing) return
    gesturing = true
    onGestureStart()
  }

  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
    if (!gesturing) return
    gesturing = false
    onGestureEnd(idleAtCameraPosition.target.useContents { MapCamera(latitude, longitude, idleAtCameraPosition.zoom) })
  }

  /** Fires on every frame of a gesture or animation: the overlay redraws against it. */
  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, didChangeCameraPosition: GMSCameraPosition) {
    camera = didChangeCameraPosition.target.useContents {
      MapCamera(latitude, longitude, didChangeCameraPosition.zoom)
    }
  }

  /** Taps that land on the map, not on a marker (we have none). */
  @ObjCSignatureOverride
  override fun mapView(mapView: GMSMapView, didTapAtCoordinate: CValue<CLLocationCoordinate2D>) {
    onTap(didTapAtCoordinate.useContents { GeoLocation(latitude, longitude) }, camera)
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
