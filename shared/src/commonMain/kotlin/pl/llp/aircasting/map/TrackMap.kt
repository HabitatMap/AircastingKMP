package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.ui.theme.LocalAqColors

/** [points] as dots joined by a path, framed to fit. Blank until there is a first point. */
@Composable
fun TrackMap(points: List<TrackPoint>, threshold: SensorThreshold?, interactive: Boolean, modifier: Modifier = Modifier) {
  BoxWithConstraints(modifier) {
    // Framed per size (inline vs full screen), not per reading: a live track grows without
    // yanking the camera away from where the user panned.
    val camera = remember(maxWidth, maxHeight, points.isNotEmpty()) {
      fitCamera(points.map { it.location }, maxWidth.value, maxHeight.value)
    } ?: return@BoxWithConstraints
    val dots = remember(points, threshold) { TrackDots.of(points, threshold) }
    val aq = LocalAqColors.current
    val levelColours = remember(aq) { MeasurementLevel.entries.map(aq::forLevel) }
    val uncoloured = MaterialTheme.colorScheme.primaryContainer // same as Explore's uncoloured dots
    val line = MaterialTheme.colorScheme.primary                 // Figma path-overlay #00668A

    PlatformMap(camera, Modifier.fillMaxSize()) { liveCamera ->
      MapDotsOverlay(dots, liveCamera, levelColours, uncoloured, line, Modifier.fillMaxSize())
    }
    // A static map: this sibling wins the hit test, so a drag here scrolls the screen, not the map.
    if (!interactive) Box(Modifier.matchParentSize().pointerInput(Unit) {})
  }
}

/** One dot where the station stands, coloured by its newest reading. A fixed view, even full screen. */
@Composable
fun StationMap(details: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier) {
  val location = details.location ?: return
  val points = remember(location, stream.value) { listOf(TrackPoint(location, stream.value?.toDouble())) }
  TrackMap(points, threshold, interactive = false, modifier)
}