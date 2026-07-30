package com.lunarlogic.aircasting.chart

import kotlin.math.abs

/** One measurement. [x] is seconds since session start; [y] is the sensor value. */
data class ChartPoint(val x: Double, val y: Double)

/**
 * Largest-Triangle-Three-Buckets downsampling to [threshold] points.
 *
 * Splits [points] into [threshold] - 2 equal buckets and keeps, from each, the point forming the
 * largest triangle with the previously kept point and the *next* bucket's centroid. Area is a proxy
 * for "how much shape is lost by dropping this point", so local extremes win — unlike every-Nth
 * sampling, which drops spikes whose index it happens to miss.
 *
 * [points] must be sorted ascending by [ChartPoint.x].
 */
fun lttb(points: List<ChartPoint>, threshold: Int): List<ChartPoint> {
  if (threshold <= 0 || threshold >= points.size) return points
  if (threshold < 3) return listOf(points.first(), points.last()).take(threshold)

  val out = ArrayList<ChartPoint>(threshold)
  // First and last are pinned, so the buckets divide the interior.
  val bucketSize = (points.size - 2).toDouble() / (threshold - 2)
  out.add(points.first())
  var previous = 0

  for (i in 0 until threshold - 2) {
    // Third triangle vertex: centroid of the next bucket. Cheaper and more stable than
    // looking at a single next point.
    val avgStart = ((i + 1) * bucketSize).toInt() + 1
    val avgEnd = (((i + 2) * bucketSize).toInt() + 1).coerceAtMost(points.size)
    var avgX = 0.0
    var avgY = 0.0
    var count = 0
    for (j in avgStart until avgEnd) {
      avgX += points[j].x
      avgY += points[j].y
      count++
    }
    if (count == 0) { // last bucket: no "next" bucket left
      avgX = points.last().x
      avgY = points.last().y
      count = 1
    }
    avgX /= count
    avgY /= count

    val rangeStart = (i * bucketSize).toInt() + 1
    val rangeEnd = (((i + 1) * bucketSize).toInt() + 1).coerceAtMost(points.size)
    val anchor = points[previous]
    var best = rangeStart.coerceAtMost(points.size - 1)
    var bestArea = -1.0
    for (j in rangeStart until rangeEnd) {
      val candidate = points[j]
      // Triangle area without the /2: we only compare, so the constant factor is dead work.
      val area = abs(
        (anchor.x - avgX) * (candidate.y - anchor.y) -
          (anchor.x - candidate.x) * (avgY - anchor.y)
      )
      if (area > bestArea) {
        bestArea = area
        best = j
      }
    }
    out.add(points[best])
    previous = best
  }

  out.add(points.last())
  return out
}
