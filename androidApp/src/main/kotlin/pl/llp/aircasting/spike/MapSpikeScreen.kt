package pl.llp.aircasting.spike

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.map.DotDrawMode
import pl.llp.aircasting.map.DotsRenderStats
import pl.llp.aircasting.map.MapCamera
import pl.llp.aircasting.map.MapDotsOverlay
import pl.llp.aircasting.map.generateTrack
import pl.llp.aircasting.map.hitTest
import pl.llp.aircasting.map.pm25Threshold
import pl.llp.aircasting.map.project
import pl.llp.aircasting.map.simplifyPath
import pl.llp.aircasting.map.WebMercator
import pl.llp.aircasting.map.worldUnitsPerPixel
import pl.llp.aircasting.ui.theme.AqColors
import kotlin.math.max
import kotlin.math.min

private enum class Renderer { CANVAS, MARKERS }
private enum class PathMode { OFF, SIMPLIFIED, FULL }

/** Above this, native markers can wedge the device for minutes — that is the finding, not a bug. */
private const val MARKER_CAP = 3_000

private val DOT_DIAMETER = 12.dp
private val PATH_COLOR = Color(0xFF9FCFE3)

@Composable
fun MapSpikeScreen() {
  var pointCount by remember { mutableIntStateOf(10_000) }
  var renderer by remember { mutableStateOf(Renderer.CANVAS) }
  var dedupeCellPx by remember { mutableFloatStateOf(1f) }
  var pathMode by remember { mutableStateOf(PathMode.SIMPLIFIED) }
  var drawMode by remember { mutableStateOf(DotDrawMode.BATCHED_POINTS) }

  val track = remember(pointCount) { generateTrack(pointCount) }
  val dots = remember(track) { track.project(pm25Threshold) }
  val levelColors = remember { MeasurementLevel.entries.map { AqColors().forLevel(it) } }

  // Simplify at ~1px of zoom 17 — good enough for the whole zoom range we care about.
  val pathPoints = remember(dots, pathMode) {
    when (pathMode) {
      PathMode.OFF -> emptyList()
      PathMode.FULL -> List(track.size) { LatLng(track.latitude[it], track.longitude[it]) }
      PathMode.SIMPLIFIED ->
        simplifyPath(dots.worldX, dots.worldY, epsilon = 1.0 / (256.0 * (1 shl 17)))
          .map { LatLng(track.latitude[it], track.longitude[it]) }
    }
  }

  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(LatLng(track.latitude[0], track.longitude[0]), 16f)
  }
  var mapLoaded by remember { mutableStateOf(false) }
  LaunchedEffect(mapLoaded, dots) {
    if (!mapLoaded) return@LaunchedEffect
    val bounds = LatLngBounds.builder()
      .apply { for (i in 0 until track.size step max(1, track.size / 500)) include(LatLng(track.latitude[i], track.longitude[i])) }
      .build()
    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
  }

  val stats = remember { DotsRenderStats() }
  var frameTick by remember { mutableIntStateOf(0) }
  var readout by remember { mutableStateOf("measuring…") }
  var tapped by remember { mutableStateOf("") }
  val density = LocalDensity.current.density

  LaunchedEffect(dots, drawMode, dedupeCellPx, renderer) {
    var previousFrame = 0L
    var frames = 0
    var totalNanos = 0L
    var worstNanos = 0L
    stats.reset()
    while (true) {
      withFrameNanos { now ->
        // Measurement only: the overlay reads this in its draw phase, so every vsync redraws
        // even with the map sitting still. Without it Compose has no work when idle,
        // `withFrameNanos` stops resuming, and the readout freezes on stale numbers.
        frameTick++
        if (previousFrame != 0L) {
          val delta = now - previousFrame
          totalNanos += delta
          worstNanos = max(worstNanos, delta)
          frames++
        }
        previousFrame = now
      }
      if (frames >= 20) {
        val averageMs = totalNanos / frames / 1_000_000.0
        readout = ("%.1f fps · avg %.1f ms · worst %.1f ms\noverlay %.2f ms " +
          "(build %.2f + draw %.2f) · drawn %d/%d")
          .format(1000.0 / averageMs, averageMs, worstNanos / 1_000_000.0,
            stats.averageBuildMillis + stats.averageDrawMillis,
            stats.averageBuildMillis, stats.averageDrawMillis,
            stats.drawnDots, dots.size)
        frames = 0; totalNanos = 0; worstNanos = 0
        stats.reset()
      }
    }
  }

  Box(Modifier.fillMaxSize()) {
    GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      onMapLoaded = { mapLoaded = true },
      // The overlay is pointer-transparent, so the map itself reports taps and we hit-test
      // them in world space against our own dots.
      onMapClick = { position ->
        val index = hitTest(
          dots = dots,
          tapWorldX = WebMercator.x(position.longitude),
          tapWorldY = WebMercator.y(position.latitude),
          toleranceWorld = 24f * worldUnitsPerPixel(cameraPositionState.position.zoom, density),
        )
        tapped = if (index >= 0) {
          "tap → #%d  %.1f µg/m³".format(index, dots.value[index])
        } else {
          "tap → nothing"
        }
      },
      // Our transform has no bearing/tilt term, so keep those gestures off.
      uiSettings = MapUiSettings(rotationGesturesEnabled = false, tiltGesturesEnabled = false),
    ) {
      if (pathPoints.isNotEmpty()) {
        Polyline(points = pathPoints, color = PATH_COLOR, width = 10f)
      }
      if (renderer == Renderer.MARKERS) {
        val icons = remember(levelColors) {
          levelColors.map { dotDescriptor(it, (DOT_DIAMETER.value * density).toInt()) }
        }
        for (i in 0 until min(dots.size, MARKER_CAP)) {
          key(i) {
            Marker(
              state = rememberUpdatedMarkerState(LatLng(track.latitude[i], track.longitude[i])),
              icon = icons[dots.level[i]],
              anchor = Offset(0.5f, 0.5f),
            )
          }
        }
      }
    }

    if (renderer == Renderer.CANVAS) {
      MapDotsOverlay(
        dots = dots,
        // Lambda, not a value: the camera is read in the draw phase, so map movement
        // invalidates drawing only — never recomposition.
        camera = {
          @Suppress("UNUSED_EXPRESSION") frameTick   // forces a redraw per vsync, see above
          val position = cameraPositionState.position
          MapCamera(position.target.latitude, position.target.longitude, position.zoom)
        },
        levelColors = levelColors,
        dotDiameter = DOT_DIAMETER,
        dedupeCellPx = dedupeCellPx,
        drawMode = drawMode,
        stats = stats,
        // No pointer-input modifier, on purpose. A Compose sibling above the map's interop
        // view wins hit-testing outright — the map would never see a gesture again.
        modifier = Modifier.fillMaxSize(),
      )
    }

    Column(
      Modifier
        .align(Alignment.TopCenter)
        .systemBarsPadding()
        .padding(8.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      ChipRow(listOf(1_000, 5_000, 10_000, 30_000).map { count ->
        Chip("${count / 1000}k", pointCount == count) { pointCount = count }
      })
      ChipRow(
        Renderer.entries.map { Chip(it.name.lowercase(), renderer == it) { renderer = it } } +
          PathMode.entries.map { Chip("path ${it.name.lowercase()}", pathMode == it) { pathMode = it } }
      )
      ChipRow(
        listOf(0f to "no dedupe", 1f to "1px", 6f to "6px").map { (cell, label) ->
          Chip(label, dedupeCellPx == cell) { dedupeCellPx = cell }
        } + listOf(
          Chip("batched", drawMode == DotDrawMode.BATCHED_POINTS) {
            drawMode = DotDrawMode.BATCHED_POINTS
          },
          Chip("circles", drawMode == DotDrawMode.PER_DOT_CIRCLES) {
            drawMode = DotDrawMode.PER_DOT_CIRCLES
          },
        )
      )
      Readout(readout)
      if (renderer == Renderer.MARKERS && dots.size > MARKER_CAP) {
        Readout("markers capped at $MARKER_CAP of ${dots.size}")
      }
      if (tapped.isNotEmpty()) Readout(tapped)
    }
  }
}

private class Chip(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun ChipRow(chips: List<Chip>) {
  Row(
    Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    chips.forEach { chip ->
      FilterChip(
        selected = chip.selected,
        onClick = chip.onClick,
        label = { Text(chip.label, fontSize = 11.sp) },
      )
    }
  }
}

@Composable
private fun Readout(text: String) {
  Text(
    text = text,
    fontSize = 11.sp,
    color = Color.White,
    modifier = Modifier
      .background(Color(0xCC000000), RoundedCornerShape(4.dp))
      .padding(horizontal = 6.dp, vertical = 3.dp),
    style = MaterialTheme.typography.labelSmall,
  )
}

private fun dotDescriptor(color: Color, diameterPx: Int): BitmapDescriptor {
  val size = max(diameterPx, 1)
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = android.graphics.Canvas(bitmap)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
  canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}
