package pl.llp.aircasting.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Every reading as a constant-size dot, joined by the path, drawn over the map (spike
 * `spike/map-dot-rendering`). [camera] is a lambda read in the draw phase: the map moving
 * invalidates drawing only — no recomposition, no layout.
 */
@Composable
fun MapDotsOverlay(
  dots: TrackDots,
  camera: () -> MapCamera,
  /** Indexed by MeasurementLevel ordinal. */
  levelColours: List<Color>,
  uncoloured: Color,
  lineColour: Color,
  modifier: Modifier = Modifier,
  dotDiameter: Dp = 16.dp, // Figma dot-6
  lineWidth: Dp = 8.dp,    // Figma path-overlay stroke
) {
  // Bucket 0 = uncoloured, bucket n + 1 = level n: one drawPoints call per colour.
  val buckets = remember(dots, levelColours.size) { List(levelColours.size + 1) { ArrayList<Offset>() } }
  val occupied = remember(dots) { PixelCellSet(dots.size) }
  val path = remember { Path() }

  Canvas(modifier) {
    if (dots.size == 0) return@Canvas
    val screen = ScreenTransform(camera(), size.width, size.height, tileSizePx = 256f * density)
    val diameter = dotDiameter.toPx()
    buckets.forEach { it.clear() }
    occupied.clear()
    path.reset()
    var lastX = 0f
    var lastY = 0f
    for (i in 0 until dots.size) {
      val x = screen.screenX(dots.worldX[i])
      val y = screen.screenY(dots.worldY[i])

      // Path: every vertex, except ones within a pixel of the previous — zoomed out, a long
      // session collapses to a few hundred segments instead of tens of thousands.
      if (i == 0) path.moveTo(x, y)
      else if (abs(x - lastX) >= 1f || abs(y - lastY) >= 1f) path.lineTo(x, y) else continue
      lastX = x
      lastY = y
      // Dots: viewport cull + one dot per screen cell.
      if (x < -diameter || x > size.width + diameter || y < -diameter || y > size.height + diameter) continue
      val cell = (x.toInt().toLong() shl 32) or (y.toInt().toLong() and 0xFFFFFFFFL)
      if (!occupied.add(cell)) continue
      buckets[dots.level[i] + 1].add(Offset(x, y))
    }

    drawPath(path, lineColour, style = Stroke(lineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    buckets.forEachIndexed { bucket, points ->
      if (points.isEmpty()) return@forEachIndexed
      val colour = if (bucket == 0) uncoloured else levelColours[bucket - 1]
      drawPoints(points, PointMode.Points, colour, strokeWidth = diameter, cap = StrokeCap.Round)
    }
    // Newest on top, whatever its colour bucket.
    val newest = dots.size - 1
    val level = dots.level[newest]
    drawCircle(
      if (level == TrackDots.Uncoloured) uncoloured else levelColours[level],
      radius = diameter / 2,
      center = Offset(screen.screenX(dots.worldX[newest]), screen.screenY(dots.worldY[newest])),
    )
  }
}

/** Open-addressed set of packed screen cells — `HashSet<Long>` boxes every key (spike, unchanged). */
internal class PixelCellSet(minimumCapacity: Int) {
  private var capacity = 16
  private val keys: LongArray

  init {
    while (capacity < minimumCapacity * 2) capacity = capacity shl 1
    keys = LongArray(capacity)
  }

  private val mask = capacity - 1

  fun clear() = keys.fill(0L)

  /** True if [cell] was not present yet. Key 0 means "empty", so everything shifts by 1. */
  fun add(cell: Long): Boolean {
    val key = cell + 1
    var index = ((key * -7046029254386353131L) ushr 40).toInt() and mask
    while (true) {
      val current = keys[index]
      if (current == 0L) { keys[index] = key; return true }
      if (current == key) return false
      index = (index + 1) and mask
    }
  }
}