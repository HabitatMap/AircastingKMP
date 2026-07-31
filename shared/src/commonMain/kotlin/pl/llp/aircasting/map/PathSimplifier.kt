package pl.llp.aircasting.map

import kotlin.math.abs

/**
 * Ramer–Douglas–Peucker simplification over projected world coordinates.
 *
 * Used for the polyline only — the dots stay untouched. Iterative (explicit stack) because a
 * 30k-point track recurses deep enough to blow the stack.
 *
 * @param epsilon max allowed deviation, in world units (`pixels / worldSizePx`).
 * @return indices of the points to keep, ascending, always including first and last.
 */
fun simplifyPath(worldX: DoubleArray, worldY: DoubleArray, epsilon: Double): IntArray {
  val n = worldX.size
  if (n <= 2) return IntArray(n) { it }

  val keep = BooleanArray(n)
  keep[0] = true
  keep[n - 1] = true

  val stack = ArrayDeque<Int>()
  stack.addLast(0)
  stack.addLast(n - 1)

  while (stack.isNotEmpty()) {
    val last = stack.removeLast()
    val first = stack.removeLast()
    if (last - first < 2) continue

    var farthest = -1
    var maxDistance = epsilon

    // Perpendicular distance to the chord, without the sqrt: compare the doubled triangle
    // area against |chord| * epsilon instead.
    val ax = worldX[first]
    val ay = worldY[first]
    val dx = worldX[last] - ax
    val dy = worldY[last] - ay
    val chord = kotlin.math.sqrt(dx * dx + dy * dy)

    for (i in first + 1 until last) {
      val distance = if (chord == 0.0) {
        val ex = worldX[i] - ax
        val ey = worldY[i] - ay
        kotlin.math.sqrt(ex * ex + ey * ey)
      } else {
        abs(dx * (ay - worldY[i]) - (ax - worldX[i]) * dy) / chord
      }
      if (distance > maxDistance) {
        maxDistance = distance
        farthest = i
      }
    }

    if (farthest != -1) {
      keep[farthest] = true
      stack.addLast(first); stack.addLast(farthest)
      stack.addLast(farthest); stack.addLast(last)
    }
  }

  var kept = 0
  for (k in keep) if (k) kept++
  val result = IntArray(kept)
  var w = 0
  for (i in 0 until n) if (keep[i]) result[w++] = i
  return result
}
