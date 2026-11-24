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
import kotlin.math.floor

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
  /** Ring the newest dot in white — the live session's current measurement. */
  markNewest: Boolean = false,
  dotDiameter: Dp = 16.dp, // Figma dot-6
  lineWidth: Dp = 4.dp,    // half Figma's 8 dp path-overlay stroke (2026-11-24)
) {
  // Per-frame scratch, reused: no allocation per dot while the map moves.
  val xs = remember(dots) { FloatArray(dots.size) }
  val ys = remember(dots) { FloatArray(dots.size) }
  val order = remember(dots) { IntArray(dots.size) }
  val occupied = remember(dots) { PixelCellSet(dots.size) }
  val run = remember { ArrayList<Offset>() }
  val path = remember { Path() }

  Canvas(modifier) {
    if (dots.size == 0) return@Canvas
    val screen = ScreenTransform(camera(), size.width, size.height, tileSizePx = 256f * density)
    val diameter = dotDiameter.toPx()

    for (i in 0 until dots.size) {
      xs[i] = screen.screenX(dots.worldX[i])
      ys[i] = screen.screenY(dots.worldY[i])
    }

    // Path: only the on-screen stretches, and no vertex within half a line width of the last —
    // a long session otherwise strokes ~200k vertices a frame, off-screen ones included.
    val lineWidthPx = lineWidth.toPx()
    path.reset()
    forEachPathVertex(xs, ys, size.width, size.height, margin = lineWidthPx, minStep = lineWidthPx / 2) { i, moveTo ->
      if (moveTo) path.moveTo(xs[i], ys[i]) else path.lineTo(xs[i], ys[i])
    }
    drawPath(path, lineColour, style = Stroke(lineWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Dots oldest → newest, so the newest lands on top (what hitTest assumes, and what the web
    // frontend does). Each run of same-coloured consecutive dots is one drawPoints call.
    fun flush(level: Int) {
      val colour = if (level == TrackDots.Uncoloured) uncoloured else levelColours[level]
      drawPoints(run, PointMode.Points, colour, strokeWidth = diameter, cap = StrokeCap.Round)
      run.clear()
    }
    // A quarter of a dot: a dropped dot sits under a newer one at most ~6 dp away, almost hidden.
    val count = topmostPerPixel(
      xs, ys, size.width, size.height, margin = diameter, cellSize = diameter / 4, occupied, order,
    )
    var runLevel = 0
    for (k in count - 1 downTo 0) {
      val i = order[k]
      if (run.isNotEmpty() && dots.level[i] != runLevel) flush(runLevel)
      runLevel = dots.level[i]
      run.add(Offset(xs[i], ys[i]))
    }
    if (run.isNotEmpty()) flush(runLevel)

    // Newest dot is drawn last above, so the ring sits on top of everything. Stroke centred on the
    // dot's edge; skipped when the newest reading is off-screen.
    val newest = dots.size - 1
    if (markNewest && count > 0 && order[0] == newest) {
      drawCircle(
        Color.White, radius = diameter / 2, center = Offset(xs[newest], ys[newest]),
        style = Stroke(CurrentDotStroke.toPx()),
      )
    }
  }
}

private val CurrentDotStroke = 2.dp

/**
 * The dots worth drawing, newest first: those within [margin] of the view, and per screen cell of
 * [cellSize] px only the newest — the one on top, the one [hitTest] picks. Writes indices into
 * [out], returns how many.
 */
internal fun topmostPerPixel(
  xs: FloatArray, ys: FloatArray, width: Float, height: Float, margin: Float, cellSize: Float,
  occupied: PixelCellSet, out: IntArray,
): Int {
  occupied.clear()
  var n = 0
  for (i in xs.size - 1 downTo 0) {
    val x = xs[i]
    val y = ys[i]
    if (x < -margin || x > width + margin || y < -margin || y > height + margin) continue
    // floor, not toInt(): toInt() truncates toward zero, making the cells at x/y = 0 double-wide.
    val cx = floor(x / cellSize).toInt()
    val cy = floor(y / cellSize).toInt()
    val cell = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
    if (occupied.add(cell)) out[n++] = i
  }
  return n
}

/**
 * The track's path vertices worth drawing, oldest → newest, reported as (index, moveTo). Skips a
 * vertex within [minStep] px of the last one drawn, and drops every segment that lies wholly
 * beyond one edge of the view (plus [margin]), resuming with a moveTo — zoomed in, nearly the
 * whole track is such segments. Inline: the draw loop pays no lambda or boxing.
 */
internal inline fun forEachPathVertex(
  xs: FloatArray, ys: FloatArray, width: Float, height: Float, margin: Float, minStep: Float,
  emit: (index: Int, moveTo: Boolean) -> Unit,
) {
  val left = -margin
  val top = -margin
  val right = width + margin
  val bottom = height + margin
  var penDown = false
  var lastX = 0f
  var lastY = 0f
  for (i in 1 until xs.size) {
    val x0 = xs[i - 1]
    val y0 = ys[i - 1]
    val x1 = xs[i]
    val y1 = ys[i]
    val offScreen = (x0 < left && x1 < left) || (x0 > right && x1 > right) ||
      (y0 < top && y1 < top) || (y0 > bottom && y1 > bottom)
    if (offScreen) { penDown = false; continue }
    if (!penDown) {
      emit(i - 1, true)
      lastX = x0
      lastY = y0
      penDown = true
    }
    if (abs(x1 - lastX) < minStep && abs(y1 - lastY) < minStep) continue
    emit(i, false)
    lastX = x1
    lastY = y1
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