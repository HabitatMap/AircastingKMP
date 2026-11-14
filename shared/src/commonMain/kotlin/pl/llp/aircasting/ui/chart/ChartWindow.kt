package pl.llp.aircasting.ui.chart

/**
 * Viewport windowing: the spike's "mandatory architectural requirement"
 * (docs/design/VICO_CHART_SPIKE_REPORT.md §5). Vico scans its series linearly every frame,
 * so it only ever gets the visible slice, decimated.
 */

/**
 * Points fed to Vico per window. The window is twice the visible width, so ~1 000 land on screen:
 * about one per horizontal pixel on a 1080 px phone, the spike's rule
 * (docs/design/VICO_CHART_SPIKE_REPORT.md). A max-zoom window holds ~242 points at 1 Hz, so it is
 * drawn raw. The spike benchmarked 800: re-benchmark on a device when raising this.
 */
internal const val POINTS_PER_WINDOW = 2_000

/**
 * Furthest zoom-in, in seconds visible: enough to tell 1 Hz samples apart (~9 px each). Not lower
 * than ~1 min: axis labels are HH:mm, so both edge labels would read the same.
 */
internal const val MIN_VISIBLE_SECONDS = 2 * 60.0

/** Extra width loaded on each side of the visible range, as a fraction of it, so short pans need no reload. */
private const val MARGIN = 0.5

internal fun windowFor(visible: ClosedFloatingPointRange<Double>): ClosedFloatingPointRange<Double> {
  val margin = visible.width * MARGIN
  return (visible.start - margin)..(visible.endInclusive + margin)
}

/** Slack on the visible width: the probe derives it from Float pixels, so it never lands exactly on a limit. */
private const val WIDTH_SLACK = 1.01


/**
 * Reload when the view leaves [loaded]; when zooming in has left fewer than half the points it was
 * decimated for; or at max zoom, when [loaded] was decimated for a wider view. The halving rule
 * can't cover that last case: zoom stops at [MIN_VISIBLE_SECONDS], short of half of any window
 * loaded for under twice that. Zooming out needs no rule of its own: the view then outgrows [loaded].
 */
internal fun needsReload(
  loaded: ClosedFloatingPointRange<Double>,
  visible: ClosedFloatingPointRange<Double>,
): Boolean {
  val loadedForWidth = loaded.width / (1 + 2 * MARGIN)
  val atMaxZoom = visible.width <= MIN_VISIBLE_SECONDS * WIDTH_SLACK
  return visible.start < loaded.start ||
    visible.endInclusive > loaded.endInclusive ||
    visible.width < loadedForWidth / 2 ||
    (atMaxZoom && loadedForWidth > visible.width * WIDTH_SLACK)
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
