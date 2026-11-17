package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.mapStyle

@Composable
actual fun PlatformMap(
  camera: MapCamera,
  modifier: Modifier,
  onTap: (at: GeoLocation, camera: MapCamera) -> Unit,
  overlay: @Composable (camera: () -> MapCamera) -> Unit,
) {
  val state = rememberCameraPositionState { position = camera.toPosition() }
  // Setting `position` moves an attached map, or seeds one that's not attached yet.
  LaunchedEffect(camera) { state.position = camera.toPosition() }
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