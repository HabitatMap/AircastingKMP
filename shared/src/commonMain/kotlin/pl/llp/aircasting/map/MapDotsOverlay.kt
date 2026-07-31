package pl.llp.aircasting.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.TimeSource

/**
 * Mutable, non-snapshot on purpose: written from the draw phase, sampled by the FPS meter.
 * Accumulates over frames — a single frame's timing is far too noisy to compare renderers.
 */
class DotsRenderStats {
  var drawnDots: Int = 0
  var frames: Int = 0

  /** Projecting, culling and bucketing — our CPU cost. */
  var buildMicros: Long = 0

  /** Issuing the draw calls — Skia's cost, dominated by fill rate. */
  var drawMicros: Long = 0

  val averageBuildMillis: Double get() = if (frames == 0) 0.0 else buildMicros / frames / 1000.0
  val averageDrawMillis: Double get() = if (frames == 0) 0.0 else drawMicros / frames / 1000.0

  fun reset() {
    frames = 0
    buildMicros = 0
    drawMicros = 0
  }
}

/** How the dots reach Skia. Both draw the same picture; only the cost differs. */
enum class DotDrawMode {
  /** One `drawPoints` per color band. Batched, but boxes an `Offset` per dot per frame. */
  BATCHED_POINTS,

  /** One `drawCircle` per dot. More draw calls, but allocation-free. */
  PER_DOT_CIRCLES,
}

/**
 * Open-addressed set of packed screen cells. A `HashSet<Long>` boxes every key, which at
 * 10k dots a frame costs more than everything else in the loop combined.
 */
internal class PixelCellSet(minimumCapacity: Int) {
  private var capacity = 16
  private val keys: LongArray

  init {
    while (capacity < minimumCapacity * 2) capacity = capacity shl 1
    keys = LongArray(capacity)
  }

  private val mask = capacity - 1

  fun clear() = keys.fill(0L)

  /** True if [cell] was not present yet. Key 0 is reserved for "empty", so everything shifts by 1. */
  fun add(cell: Long): Boolean {
    val key = cell + 1
    var index = ((key * -7046029254386353131L) ushr 40).toInt() and mask
    while (true) {
      val current = keys[index]
      if (current == 0L) {
        keys[index] = key
        return true
      }
      if (current == key) return false
      index = (index + 1) and mask
    }
  }
}

/**
 * Draws every measurement as a constant-size dot on top of the map — the native equivalent of
 * the web app's `OverlayView` + `fromLatLngToDivPixel` (one div per measurement).
 *
 * Two things make this cheap:
 * - [camera] is a lambda, so the camera state is read **inside the draw phase**. The map moving
 *   invalidates drawing only — no recomposition, no layout.
 * - dots are batched into one `drawPoints` call per color (round caps == circles), instead of
 *   N draw ops or N map objects.
 *
 * @param dedupeCellPx collapse dots landing in the same screen cell of this size; 0 disables.
 *   Zoomed out, thousands of measurements stack on the same pixels — this is the cull that
 *   actually pays. It is a screen-space, zoom-aware version of the web app's
 *   "drop identical lat/lng" dedup.
 */
@Composable
fun MapDotsOverlay(
  dots: TrackDots,
  camera: () -> MapCamera,
  levelColors: List<Color>,
  modifier: Modifier = Modifier,
  dotDiameter: Dp = 12.dp,
  dedupeCellPx: Float = 1f,
  drawMode: DotDrawMode = DotDrawMode.BATCHED_POINTS,
  stats: DotsRenderStats? = null,
) {
  val buckets = remember(levelColors.size, dots) {
    List(levelColors.size) { ArrayList<Offset>(dots.size / levelColors.size + 16) }
  }
  val occupied = remember(dots) { PixelCellSet(dots.size) }

  Canvas(modifier) {
    val buildStartedAt = TimeSource.Monotonic.markNow()
    val transform = ScreenTransform(
      camera = camera(),
      widthPx = size.width,
      heightPx = size.height,
      tileSizePx = 256f * density,
    )

    val batched = drawMode == DotDrawMode.BATCHED_POINTS
    if (batched) buckets.forEach { it.clear() }
    occupied.clear()

    val diameter = dotDiameter.toPx()
    val radius = diameter / 2f
    val margin = diameter
    var drawn = 0
    var drawMicros = 0L

    for (i in 0 until dots.size) {
      val x = transform.screenX(dots.worldX[i])
      if (x < -margin || x > size.width + margin) continue
      val y = transform.screenY(dots.worldY[i])
      if (y < -margin || y > size.height + margin) continue

      if (dedupeCellPx > 0f) {
        val cellX = (x / dedupeCellPx).toInt().toLong()
        val cellY = (y / dedupeCellPx).toInt().toLong()
        if (!occupied.add((cellX shl 32) or (cellY and 0xFFFFFFFFL))) continue
      }

      if (batched) {
        // Boxes an Offset — the price of the batched API.
        buckets[dots.level[i]].add(Offset(x, y))
      } else {
        drawCircle(levelColors[dots.level[i]], radius, Offset(x, y))
      }
      drawn++
    }

    val buildMicros = buildStartedAt.elapsedNow().inWholeMicroseconds

    if (batched) {
      val drawStartedAt = TimeSource.Monotonic.markNow()
      for (level in buckets.indices) {
        val points = buckets[level]
        if (points.isEmpty()) continue
        drawPoints(
          points = points,
          pointMode = PointMode.Points,
          color = levelColors[level],
          strokeWidth = diameter,
          cap = StrokeCap.Round,
        )
      }
      drawMicros = drawStartedAt.elapsedNow().inWholeMicroseconds
    }

    stats?.let {
      it.drawnDots = drawn
      it.frames++
      it.buildMicros += buildMicros
      it.drawMicros += drawMicros
    }
  }
}

/**
 * Nearest dot to a tapped position, or -1.
 *
 * Works in world coordinates, not screen coordinates, so the overlay can stay completely
 * pointer-transparent: the map reports the tap (`onMapClick` / `didTapAtCoordinate`) and we
 * answer it. Screen-space hit-testing would need a pointer-input modifier over the map, and a
 * Compose sibling above an interop view swallows the gestures the map needs.
 *
 * @param toleranceWorld tap radius in world units — `tolerancePx * worldUnitsPerPixel(...)`.
 */
fun hitTest(
  dots: TrackDots,
  tapWorldX: Double,
  tapWorldY: Double,
  toleranceWorld: Double,
): Int {
  var best = -1
  var bestDistance = toleranceWorld * toleranceWorld
  for (i in 0 until dots.size) {
    val dx = dots.worldX[i] - tapWorldX
    if (dx < -toleranceWorld || dx > toleranceWorld) continue
    val dy = dots.worldY[i] - tapWorldY
    if (dy < -toleranceWorld || dy > toleranceWorld) continue
    val distance = dx * dx + dy * dy
    if (distance <= bestDistance) {
      bestDistance = distance
      best = i
    }
  }
  return best
}
