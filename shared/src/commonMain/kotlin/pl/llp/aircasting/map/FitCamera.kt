package pl.llp.aircasting.map

import pl.llp.aircasting.domain.GeoLocation
import kotlin.math.log2

/** Legacy `MapContainer.DEFAULT_ZOOM`: a track never opens closer than street level. */
const val MaxFitZoom = 16f

/** Google zoom levels: the world is 256 dp wide at zoom 0, and doubles per level. */
private const val TileDp = 256.0

/**
 * The camera that frames [points] inside a [widthDp] × [heightDp] view, leaving [paddingDp] on
 * every side — legacy `newLatLngBounds(bounds, padding)` + the [MaxFitZoom] cap, done in shared
 * code so both platforms only need "centre + zoom". Null when there is nothing to frame.
 */
fun fitCamera(
  points: List<GeoLocation>,
  widthDp: Float,
  heightDp: Float,
  paddingDp: Float = 32f
): MapCamera? {
  if (points.isEmpty()) return null
  val xs = points.map { WebMercator.x(it.longitude) }
  val ys = points.map { WebMercator.y(it.latitude) }
  val (minX, maxX) = xs.min() to xs.max()
  val (minY, maxY) = ys.min() to ys.max()

  // A zero span divides to +∞, so a single point falls through to the cap.
  fun zoomFor(viewDp: Float, span: Double) =
    log2((viewDp - 2 * paddingDp).coerceAtLeast(1f) / (TileDp * span))

  val zoom =
    minOf(zoomFor(widthDp, maxX - minX), zoomFor(heightDp, maxY - minY), MaxFitZoom.toDouble())

  // Centre in projected space, not lat/lng space — the same centre Google's bounds fit picks.
  return MapCamera(
    centerLatitude = WebMercator.latitude((minY + maxY) / 2),
    centerLongitude = WebMercator.longitude((minX + maxX) / 2),
    zoom = zoom.toFloat(),
  )
}