package pl.llp.aircasting.map

import pl.llp.aircasting.domain.SensorThreshold

/** A recorded mobile track: one measurement per index. */
class Track(
  val latitude: DoubleArray,
  val longitude: DoubleArray,
  val value: DoubleArray,
) {
  val size: Int get() = latitude.size
}

/**
 * A track pre-projected for drawing: parallel arrays, no per-point objects.
 *
 * Projection happens once when the track loads; the render loop only scales and translates.
 */
class TrackDots(
  val worldX: DoubleArray,
  val worldY: DoubleArray,
  /** [pl.llp.aircasting.domain.MeasurementLevel] ordinal, used as a color index. */
  val level: IntArray,
  val value: DoubleArray,
) {
  val size: Int get() = worldX.size
}

fun Track.project(threshold: SensorThreshold): TrackDots = TrackDots(
  worldX = DoubleArray(size) { WebMercator.x(longitude[it]) },
  worldY = DoubleArray(size) { WebMercator.y(latitude[it]) },
  level = IntArray(size) { threshold.levelFor(value[it]).ordinal },
  value = value,
)
