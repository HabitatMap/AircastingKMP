package pl.llp.aircasting.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.ui.theme.LocalAqColors


/** [points] as dots joined by a path, framed to fit; tap a dot for its reading. Blank until a first point. */
@Composable
fun TrackMap(
  points: List<TrackPoint>, threshold: SensorThreshold?, unit: String, timeZone: TimeZone,
  interactive: Boolean, modifier: Modifier = Modifier,
) {
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
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = selected?.takeIf { it < dots.size }

    PlatformMap(
      camera,
      Modifier.fillMaxSize(),
      onTap = { at, now ->
        selected = hitTest(
          dots, WebMercator.x(at.longitude), WebMercator.y(at.latitude),
          // worldUnitsPerPixel at density 1 = world units per dp.
          toleranceWorld = TapRadiusDp * worldUnitsPerPixel(now.zoom, densityScale = 1f),
        )
      },
    ) { liveCamera ->
      MapDotsOverlay(dots, liveCamera, levelColours, uncoloured, line, Modifier.fillMaxSize())
      shown?.let { i ->
        MapTooltip(
          points[i], unit, timeZone,
          levelColour = dots.level[i].takeIf { it != TrackDots.Uncoloured }
            ?.let(levelColours::get),
          onClose = { selected = null },
          modifier = Modifier.anchoredTo(dots, i, liveCamera),
        )
      }
    }
    // A static map: this sibling wins the hit test, so a drag here scrolls the screen, not the map.
    if (!interactive) Box(
      Modifier.matchParentSize()
        .pointerInput(Unit) {})
  }
}

/** Dot radius (8 dp) plus a fingertip's slack. */
private const val TapRadiusDp = 16

/** One dot where the station stands, coloured by its newest reading. A fixed view, even full screen. */
@Composable
fun StationMap(details: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier) {
  val location = details.location ?: return
  val points = remember(location, stream.value, stream.measuredAt) {
    listOf(TrackPoint(location, stream.value?.toDouble(), stream.measuredAt))
  }
  TrackMap(points, threshold, stream.unitSymbol, details.timeZone, interactive = false, modifier)
}