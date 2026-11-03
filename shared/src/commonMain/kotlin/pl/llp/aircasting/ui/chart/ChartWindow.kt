package pl.llp.aircasting.ui.chart

/**
 * Viewport windowing: the spike's "mandatory architectural requirement"
 * (docs/design/VICO_CHART_SPIKE_REPORT.md §5). Vico scans its series linearly every frame,
 * so it only ever gets the visible slice, decimated.
 */

/** Points fed to Vico per window: ~600 on screen, since the window is twice the visible width. */
internal const val POINTS_PER_WINDOW = 1_200

/** Extra width loaded on each side of the visible range, as a fraction of it, so short pans need no reload. */
private const val MARGIN = 0.5

internal fun windowFor(visible: ClosedFloatingPointRange<Double>): ClosedFloatingPointRange<Double> {
  val margin = visible.width * MARGIN
  return (visible.start - margin)..(visible.endInclusive + margin)
}

/**
 * Reload when the view leaves [loaded], or when zooming in has left fewer than half the points it
 * was decimated for. Zooming out needs no rule of its own: the view then outgrows [loaded].
 */
internal fun needsReload(
  loaded: ClosedFloatingPointRange<Double>,
  visible: ClosedFloatingPointRange<Double>,
): Boolean {
  val loadedForWidth = loaded.width / (1 + 2 * MARGIN)
  return visible.start < loaded.start ||
    visible.endInclusive > loaded.endInclusive ||
    visible.width < loadedForWidth / 2
}

/**
 * [points] (sorted by x) inside [range], plus one neighbour on each side so the line runs to the
 * edge. Binary search, not a scan: this runs on every reload over up to 86 400 points.
 * Returns a view of [points], so no copy.
 */
internal fun pointsIn(points: List<ChartPoint>, range: ClosedFloatingPointRange<Double>): List<ChartPoint> {
  val from = (points.lowerBound(range.start) - 1).coerceAtLeast(0)
  val to = (points.lowerBound(range.endInclusive) + 1).coerceAtMost(points.size)
  return points.subList(from, to)
}

internal fun pointsToDraw(points: List<ChartPoint>, window: ClosedFloatingPointRange<Double>): List<ChartPoint> =
  lttb(pointsIn(points, window), POINTS_PER_WINDOW)

/** Index of the first point with x ≥ [x]; [List.size] when there is none. */
private fun List<ChartPoint>.lowerBound(x: Double): Int {
  val i = binarySearch { it.x.compareTo(x) }
  return if (i >= 0) i else -(i + 1)
}

private val ClosedFloatingPointRange<Double>.width get() = endInclusive - start
