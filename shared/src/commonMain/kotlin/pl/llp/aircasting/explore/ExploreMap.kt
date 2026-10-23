package pl.llp.aircasting.explore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType

/**
 * The platform map: Google Maps on Android, MapKit on iOS (KMP_ARCHITECTURE.md §6 — maps stay
 * native). Draws [markers] and reports the visible area each time the camera comes to rest.
 */
@Composable
expect fun ExploreMap(
  markers: List<ExploreMarker>,
  colourType: MeasurementType?,
  onCameraIdle: (MapViewport) -> Unit,
  modifier: Modifier = Modifier,
)