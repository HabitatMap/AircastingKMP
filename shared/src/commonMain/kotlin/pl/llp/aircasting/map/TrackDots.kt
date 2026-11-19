package pl.llp.aircasting.map

import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.TrackPoint

/**
 * A track pre-projected for drawing: parallel arrays, no per-point objects (spike finding: at
 * 10k+ points, per-dot objects dominate the frame). Built once per (track, threshold); the draw
 * loop only scales and translates.
 */
class TrackDots private constructor(
  val worldX: DoubleArray,
  val worldY: DoubleArray,
  /** [pl.llp.aircasting.domain.MeasurementLevel] ordinal, or [Uncoloured]. */
  val level: IntArray,
) {
  val size: Int get() = worldX.size
  companion object {
    const val Uncoloured = -1

    fun of(points: List<TrackPoint>, threshold: SensorThreshold?) = TrackDots(
      worldX = DoubleArray(points.size) { WebMercator.x(points[it].location.longitude) },
      worldY = DoubleArray(points.size) { WebMercator.y(points[it].location.latitude) },
      level = IntArray(points.size) { i ->
        points[i].value?.let { threshold?.levelFor(it)?.ordinal } ?: Uncoloured
      },
    )
  }
}
