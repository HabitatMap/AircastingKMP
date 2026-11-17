package pl.llp.aircasting.map

import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HitTestTest {
  // Dot 0 at 0°, dots 1 and 2 stacked at 0.001° east (a pause in the track).
  private val dots = TrackDots.of(
    listOf(
      TrackPoint(GeoLocation(0.0, 0.0), 1.0),
      TrackPoint(GeoLocation(0.0, 0.001), 2.0),
      TrackPoint(GeoLocation(0.0, 0.001), 3.0),
    ),
    threshold = null,
  )
  private val tolerance = 1e-5 // world units: ≈ 0.0036° of longitude

  @Test
  fun picks_the_nearest_dot_within_reach() {
    assertEquals(0, hitTest(dots, WebMercator.x(0.0002), WebMercator.y(0.0), tolerance))
  }

  @Test
  fun a_tap_away_from_every_dot_hits_nothing() {
    assertNull(hitTest(dots, WebMercator.x(0.01), WebMercator.y(0.0), tolerance))
  }

  @Test
  fun stacked_dots_resolve_to_the_newest_the_one_drawn_on_top() {
    assertEquals(2, hitTest(dots, WebMercator.x(0.001), WebMercator.y(0.0), tolerance))
  }
}