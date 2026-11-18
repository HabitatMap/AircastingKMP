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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.ui.components.StationPill
import pl.llp.aircasting.ui.theme.LocalAqColors


/** [points] as dots joined by a path, framed to fit; tap a dot for its reading. Blank until a first point. */
@Composable
fun TrackMap(
  points: List<TrackPoint>, threshold: SensorThreshold?, unit: String, timeZone: TimeZone,
  interactive: Boolean, modifier: Modifier = Modifier,
) {
  // Clip: Compose doesn't clip drawing to layout bounds, and the overlay Canvas draws the whole
  // path, off-screen parts included, over whatever sits next to the map.
  BoxWithConstraints(modifier.clipToBounds()) {
    // Framed per size (inline vs full screen), not per reading: a live track grows without
    // yanking the camera away from where the user panned.
    val camera = remember(maxWidth, maxHeight, points.isNotEmpty()) {
      fitCamera(points.map { it.location }, maxWidth.value, maxHeight.value)
    } ?: return@BoxWithConstraints
    val dots = remember(points, threshold) { TrackDots.of(points, threshold) }
    val aq = LocalAqColors.current
    val levelColours = remember(aq) { MeasurementLevel.entries.map(aq::forLevel) }
    val uncoloured = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.primaryContainer
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = selected?.takeIf { it < dots.size }

    PlatformMap(
      camera,
      Modifier.fillMaxSize(),
      onTap = { at, now ->
        // worldUnitsPerPixel at density 1 = world units per dp.
        val worldPerDp = worldUnitsPerPixel(now.zoom, densityScale = 1f)
        selected = hitTest(
          dots, WebMercator.x(at.longitude), WebMercator.y(at.latitude),
          dotRadiusWorld = DotRadiusDp * worldPerDp,
          toleranceWorld = TapRadiusDp * worldPerDp,
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

/** Half MapDotsOverlay's default dotDiameter: the disk a tap must land in to hit "that" dot. */
private const val DotRadiusDp = 8

/** Dot radius (8 dp) plus a fingertip's slack. */
private const val TapRadiusDp = 16

/**
 * Figma 1138:28370: the station's pill (as on Explore) with its newest reading, coloured by
 * [threshold]. A fixed view, even full screen: the camera centres on the station and never moves,
 * so the pill just sits in the middle — no projection needed.
 */
@Composable
fun StationMap(details: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier) {
  val location = details.location ?: return
  BoxWithConstraints(modifier.clipToBounds()) {
    val camera = remember(location, maxWidth, maxHeight) {
      fitCamera(listOf(location), maxWidth.value, maxHeight.value)
    } ?: return@BoxWithConstraints
    val level = stream.value?.let { value -> threshold?.levelFor(value.toDouble()) }
    val label = stream.value?.let { "$it ${stream.unitSymbol}" } ?: "– ${stream.unitSymbol}"
    PlatformMap(camera, Modifier.fillMaxSize(), onTap = { _, _ -> }) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        StationPill(
          label,
          level?.let(LocalAqColors.current::forLevel),
          airBeam = details.source == DataSource.AirBeam,
        )
      }
    }
    // Static, as TrackMap's inline map: drags scroll the screen.
    Box(Modifier.matchParentSize().pointerInput(Unit) {})
  }
}
