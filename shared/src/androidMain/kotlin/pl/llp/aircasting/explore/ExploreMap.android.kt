package pl.llp.aircasting.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.filterNotNull
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.GeoSquare
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.ui.theme.LocalAqColors
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties

@Composable
actual fun ExploreMap(
  markers: List<ExploreMarker>,
  colourType: MeasurementType?,
  startCentre: GeoLocation?,
  onCameraIdle: (MapViewport) -> Unit,
  modifier: Modifier,
) {
  val camera = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(START.latitude, START.longitude), START_ZOOM)
  }
  val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
  LaunchedEffect(camera) {
    // Re-runs when the camera stops moving, and once when the map attaches (projection turns non-null).
    snapshotFlow {
      if (camera.isMoving) null
      else camera.projection?.visibleRegion?.latLngBounds?.let { bounds ->
        MapViewport(
          GeoSquare(
            north = bounds.northeast.latitude,
            south = bounds.southwest.latitude,
            east = bounds.northeast.longitude,
            west = bounds.southwest.longitude,
          ),
          zoom = camera.position.zoom.toDouble(),
        )
      }
    }
      .filterNotNull()
      .collect { currentOnCameraIdle(it) }
  }
  LaunchedEffect(startCentre) {
    startCentre?.let { camera.move(CameraUpdateFactory.newLatLngZoom(it.toLatLng(), USER_ZOOM)) }
  }
  GoogleMap(
    modifier = modifier,
    cameraPositionState = camera,
    properties = MapProperties(mapStyleOptions = MapStyleOptions(mapStyle())),
    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
  ) {
    markers.forEach { marker ->
      when (marker) {
        is ExploreMarker.Dot -> key(marker) {
          MarkerComposable(
            marker,
            state = rememberUpdatedMarkerState(marker.location.toLatLng()),
            anchor = CENTER,
          ) { ClusterDot() }
        }

        is ExploreMarker.Station -> key(marker.station.uuid) {
          val label = marker.label(colourType)
          MarkerComposable(
            label, marker,
            state = rememberUpdatedMarkerState(marker.station.location.toLatLng()),
            anchor = CENTER,
          ) {
            StationPill(label, marker.level?.let(LocalAqColors.current::forLevel))
          }
        }
      }
    }
  }
}

/** Halo + white pill with a coloured dot and the label; blue (primaryContainer) when uncoloured. */
@Composable
private fun StationPill(label: String, levelColour: Color?) {
  val colour = levelColour ?: MaterialTheme.colorScheme.primaryContainer
  Box(contentAlignment = Alignment.Center) {
    Box(Modifier.size(56.dp).background(colour.copy(alpha = 0.4f), CircleShape))
    Surface(shape = RoundedCornerShape(50), shadowElevation = 2.dp, color = Color.White) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(Modifier.size(12.dp).background(colour, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Black)
      }
    }
  }
}

@Composable
private fun ClusterDot() {
  val colour = MaterialTheme.colorScheme.primaryContainer
  Box(Modifier.size(28.dp).background(colour.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
    Box(Modifier.size(16.dp).background(colour, CircleShape))
  }
}

private fun GeoLocation.toLatLng() = LatLng(latitude, longitude)

private val CENTER = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
private val START = GeoLocation(39.8, -98.6) // contiguous US
private const val START_ZOOM = 3f
private const val USER_ZOOM = 10f