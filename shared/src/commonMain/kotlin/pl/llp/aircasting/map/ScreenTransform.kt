package pl.llp.aircasting.map

import kotlin.math.pow

/** The part of the map camera we need to place points ourselves. Bearing/tilt unsupported. */
data class MapCamera(
  val centerLatitude: Double,
  val centerLongitude: Double,
  val zoom: Float,
)

/**
 * Maps unit-square world coordinates to screen pixels for one frame.
 *
 * At zoom z the whole world is `tileSizePx * 2^z` pixels wide (Google's tile scheme, 256dp
 * tiles). So the transform is a single scale + translate — no per-point SDK call, no JNI.
 *
 * Note: world coordinates must stay `Double`. At zoom 20 the world is ~2.7e8 px wide, and
 * `Float` precision there is ~16 px of error.
 */
class ScreenTransform(
  camera: MapCamera,
  widthPx: Float,
  heightPx: Float,
  tileSizePx: Float,
) {
  val worldSizePx: Double = tileSizePx.toDouble() * 2.0.pow(camera.zoom.toDouble())

  private val originX = WebMercator.x(camera.centerLongitude) * worldSizePx - widthPx / 2.0
  private val originY = WebMercator.y(camera.centerLatitude) * worldSizePx - heightPx / 2.0

  fun screenX(worldX: Double): Float = (worldX * worldSizePx - originX).toFloat()

  fun screenY(worldY: Double): Float = (worldY * worldSizePx - originY).toFloat()
}

/** How much of the unit square one screen pixel covers at this zoom. */
fun worldUnitsPerPixel(zoom: Float, densityScale: Float): Double =
  1.0 / (256.0 * densityScale * 2.0.pow(zoom.toDouble()))
