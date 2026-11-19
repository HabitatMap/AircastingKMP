package pl.llp.aircasting.map

/**
 * The dot a tap lands on, or null. Works in world coordinates, so the overlay stays
 * pointer-transparent: the map reports the tap, and we answer it (spike `spike/map-dot-rendering`).
 * Dots are drawn oldest → newest, so the newest dot whose disk covers the tap is the one the user
 * sees there (the web frontend gets the same from DOM hit-testing). A tap on no disk falls back to
 * the nearest dot within [toleranceWorld]; newest wins a tie.
 */
fun hitTest(
  dots: TrackDots, tapWorldX: Double, tapWorldY: Double,
  dotRadiusWorld: Double, toleranceWorld: Double,
): Int? {
  val radiusSquared = dotRadiusWorld * dotRadiusWorld
  var nearest: Int? = null
  var nearestDistance = toleranceWorld * toleranceWorld
  for (i in dots.size - 1 downTo 0) { // newest first = topmost first
    // Cheap box reject before the multiply.
    val dx = dots.worldX[i] - tapWorldX
    if (dx < -toleranceWorld || dx > toleranceWorld) continue
    val dy = dots.worldY[i] - tapWorldY
    if (dy < -toleranceWorld || dy > toleranceWorld) continue
    val distance = dx * dx + dy * dy
    if (distance <= radiusSquared) return i // on this dot, and nothing newer covers the tap
    if (distance < nearestDistance) { // strict: on a tie the newer one, seen first, stays
      nearestDistance = distance
      nearest = i
    }
  }
  return nearest
}
