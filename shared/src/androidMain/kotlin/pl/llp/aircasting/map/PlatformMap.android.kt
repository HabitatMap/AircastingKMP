package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.drop
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.mapStyle

@Composable
actual fun PlatformMap(
  camera: MapCamera,
  modifier: Modifier,
  onTap: (at: GeoLocation, camera: MapCamera) -> Unit,
  onGestureStart: () -> Unit,
  onGestureEnd: (camera: MapCamera) -> Unit,
  overlay: @Composable (camera: () -> MapCamera) -> Unit,
) {
  val state = rememberCameraPositionState { position = camera.toPosition() }
  // Setting `position` moves an attached map, or seeds one that's not attached yet.
  LaunchedEffect(camera) { state.position = camera.toPosition() }
  val currentOnGestureStart by rememberUpdatedState(onGestureStart)
  val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
  LaunchedEffect(state) {
    // maps-compose sets the reason and isMoving together in OnCameraMoveStarted, and clears
    // isMoving on idle. Only GESTURE moves are the user's: ours (setting `position`) are DEVELOPER.
    snapshotFlow { state.isMoving }.drop(1).collect { moving ->
      if (state.cameraMoveStartedReason != CameraMoveStartedReason.GESTURE) return@collect
      if (moving) currentOnGestureStart() else currentOnGestureEnd(state.position.toMapCamera())
    }
  }
  val style = mapStyle()
  val properties = remember(style) { MapProperties(mapStyleOptions = MapStyleOptions(style)) }

  Box(modifier) {
    GoogleMap(
      modifier = Modifier.matchParentSize(),
      cameraPositionState = state,
      properties = properties,
      uiSettings = MapUiSettings(
        compassEnabled = false,
        mapToolbarEnabled = false,
        myLocationButtonEnabled = false,
        rotationGesturesEnabled = false,
        tiltGesturesEnabled = false,
        zoomControlsEnabled = false,
      ),
      onMapClick = { onTap(GeoLocation(it.latitude, it.longitude), state.position.toMapCamera()) },
      )
    // `position` is snapshot state that maps-compose updates on every camera frame.
    overlay { state.position.toMapCamera() }
  }
}

private fun MapCamera.toPosition() = CameraPosition.fromLatLngZoom(LatLng(centerLatitude, centerLongitude), zoom)
private fun CameraPosition.toMapCamera() = MapCamera(target.latitude, target.longitude, zoom)