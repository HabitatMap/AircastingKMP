package pl.llp.aircasting.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pl.llp.aircasting.domain.GeoLocation

/**
 * A styled Google map (Maps SDK on Android, Maps iOS SDK on iOS — KMP_ARCHITECTURE.md §6) with no
 * markers of its own: [overlay] draws on top, reading the live camera in its draw phase.
 * Moves to [camera] whenever it changes. Rotation and tilt are off: ScreenTransform only
 * knows centre + zoom.
 */
@Composable
expect fun PlatformMap(
  camera: MapCamera,
  modifier: Modifier = Modifier,
  /** A tap on the map itself, with the camera at that moment (for the tap radius in world units). */
  onTap: (at: GeoLocation, camera: MapCamera) -> Unit,
  overlay: @Composable (camera: () -> MapCamera) -> Unit,
)