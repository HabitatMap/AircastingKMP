package pl.llp.aircasting.map

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sin

/**
 * Web-Mercator (EPSG:3857) normalized to the unit square — the projection Google Maps uses.
 *
 * x = 0 at -180°, 1 at +180°; y = 0 at the north edge, 1 at the south edge.
 * Projecting once into this space lets every frame reduce to a scale + translate
 * (see [ScreenTransform]) instead of thousands of SDK projection calls.
 */
object WebMercator {
  /** Latitude cutoff where Mercator y would diverge — Google clamps to the same value. */
  const val MAX_LATITUDE = 85.05112878

  fun x(longitudeDeg: Double): Double = (longitudeDeg + 180.0) / 360.0

  fun y(latitudeDeg: Double): Double {
    val s = sin(latitudeDeg.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180.0)
    return 0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)
  }
}
