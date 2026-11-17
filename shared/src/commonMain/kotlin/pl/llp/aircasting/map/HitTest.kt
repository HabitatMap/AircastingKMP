package pl.llp.aircasting.map

/**
 * The dot nearest a tap, or null. Works in world coordinates, so the overlay stays
 * pointer-transparent: the map reports the tap, and we answer it (spike `spike/map-dot-rendering`).
 * Ties go to the later index — the newest reading, drawn on top.
 *
 * @param toleranceWorld tap radius in world units.
 */
 fun hitTest(dots: TrackDots, tapWorldX: Double, tapWorldY: Double, toleranceWorld: Double): Int? {
   var best: Int? = null
   var bestDistance = toleranceWorld * toleranceWorld
   for (i in 0 until dots.size) {
     // Cheap box reject before the multiply.
     val dx = dots.worldX[i] - tapWorldX
     if (dx < -toleranceWorld || dx > toleranceWorld) continue
     val dy = dots.worldY[i] - tapWorldY
     if (dy < -toleranceWorld || dy > toleranceWorld) continue
     val distance = dx * dx + dy * dy
     if (distance <= bestDistance) {
       bestDistance = distance
       best = i
     }
   }
   return best
 }