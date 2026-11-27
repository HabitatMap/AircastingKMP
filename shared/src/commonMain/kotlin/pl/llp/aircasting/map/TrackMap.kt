package pl.llp.aircasting.map

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_note
import aircasting.shared.generated.resources.ic_my_location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.StationPill
import pl.llp.aircasting.ui.theme.LocalAqColors


/**
 * [points] as dots joined by a path; tap a dot for its reading. Blank until a first point.
 * Pans and zooms inline and full screen alike. [userCamera] (null = the default view) and
 * [onUserMoved] are hoisted so the view survives the map leaving composition (graph tab,
 * full-screen toggle). [live] = an active session: the default view centres on the newest point
 * (the user's location) with the whole track in view, re-framed on every reading so the map
 * follows the user, and a Centre button returns to it. Otherwise the default view frames the track.
 * [followZoom]: a zoom the user chose — the default view keeps it rather than fitting the track.
 */
@Composable
fun TrackMap(
  points: List<TrackPoint>, threshold: SensorThreshold?, unit: String, timeZone: TimeZone,
  live: Boolean, userCamera: MapCamera?, followZoom: Float?, onUserMoved: (MapCamera) -> Unit, onRecentre: () -> Unit,
  modifier: Modifier = Modifier,
  /** Where the session's notes were written: one marker each, above the track. */
  notes: List<GeoLocation> = emptyList(),
) {
  // Clip: Compose doesn't clip drawing to layout bounds, and the overlay Canvas draws the whole
  // path, off-screen parts included, over whatever sits next to the map.
  BoxWithConstraints(modifier.clipToBounds()) {
    val defaultCamera = remember(points, live, followZoom, maxWidth, maxHeight) {
      val locations = points.map { it.location }
      val fit = fitCamera(locations, maxWidth.value, maxHeight.value, centre = locations.lastOrNull()?.takeIf { live })
      followZoom?.let { fit?.copy(zoom = it) } ?: fit
    } ?: return@BoxWithConstraints
    val camera = userCamera ?: defaultCamera
    val dots = remember(points, threshold) { TrackDots.of(points, threshold) }
    val aq = LocalAqColors.current
    val levelColours = remember(aq) { MeasurementLevel.entries.map(aq::forLevel) }
    val uncoloured = MaterialTheme.colorScheme.primary
    // Half-transparent so the map's streets show through the path (2026-11-24).
    val line = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = selected?.takeIf { it < dots.size }

    PlatformMap(
      camera,
      Modifier.fillMaxSize(),
      // Freeze on the view the gesture starts from — the same value as `camera`, so the map isn't
      // moved — so the next reading doesn't re-frame it mid-gesture. Then keep where it settles.
      onGestureStart = { onUserMoved(camera) },
      onGestureEnd = onUserMoved,
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
      MapDotsOverlay(dots, liveCamera, levelColours, uncoloured, line, Modifier.fillMaxSize(), markNewest = live)
      notes.forEach { NoteMarker(Modifier.centredOn(WebMercator.x(it.longitude), WebMercator.y(it.latitude), liveCamera)) }
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
    if (live) MapButton(
      Res.drawable.ic_my_location,
      LocalStrings.current.myLocation,
      onClick = onRecentre,
      Modifier.align(Alignment.BottomEnd).padding(4.dp),
    )
  }
}

/** The graph's "Timeline note indicator" (Figma 1147:351), as a map pin: 24 dp disc, 12 dp note icon. */
@Composable
private fun NoteMarker(modifier: Modifier) {
  Box(
    modifier.size(24.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(painterResource(Res.drawable.ic_note), contentDescription = null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** Half MapDotsOverlay's default dotDiameter: the disk a tap must land in to hit "that" dot. */
private const val DotRadiusDp = 8

/** Dot radius (8 dp) plus a fingertip's slack. */
private const val TapRadiusDp = 16

/**
 * Figma 1138:28370: the station's pill (as on Explore) with its newest reading, coloured by
 * [threshold]. Framed on the station; [interactive] (full screen) lets the user pan and zoom, so the
 * pill is pinned to the station's projected position rather than to the middle of the view.
 */
@Composable
fun StationMap(
  details: StationDetails, stream: StationStream, threshold: SensorThreshold?,
  interactive: Boolean, modifier: Modifier = Modifier,
) {
  val location = details.location ?: return
  BoxWithConstraints(modifier.clipToBounds()) {
    val camera = remember(location, maxWidth, maxHeight) {
      fitCamera(listOf(location), maxWidth.value, maxHeight.value)
    } ?: return@BoxWithConstraints
    val level = stream.value?.let { value -> threshold?.levelFor(value.toDouble()) }
    val label = stream.value?.let { "$it ${stream.unitSymbol}" } ?: "– ${stream.unitSymbol}"
    PlatformMap(camera, Modifier.fillMaxSize(), onTap = { _, _ -> }) { liveCamera ->
      StationPill(
        label,
        level?.let(LocalAqColors.current::forLevel),
        airBeam = details.source == DataSource.AirBeam,
        modifier = Modifier.centredOn(WebMercator.x(location.longitude), WebMercator.y(location.latitude), liveCamera),
      )
    }
    // Static inline: this sibling wins the hit test, so a drag here scrolls the screen, not the map.
    if (!interactive) Box(Modifier.matchParentSize().pointerInput(Unit) {})
  }
}
