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
  private val radius = tolerance / 2 // ≈ 0.0018°: dot 0 and dots 1–2 overlap

  @Test
  fun a_tap_on_a_dot_alone_picks_it() {
    // 0.001° west of dot 0: inside its disk, outside dot 1's.
    assertEquals(0, hitTest(dots, WebMercator.x(-0.001), WebMercator.y(0.0), radius, tolerance))
  }

  @Test
  fun a_tap_where_dots_overlap_picks_the_one_drawn_on_top_not_the_nearest() {
    // Nearer dot 0's centre, but inside newer dot 2's disk, which covers dot 0 there.
    assertEquals(2, hitTest(dots, WebMercator.x(0.0002), WebMercator.y(0.0), radius, tolerance))
  }

  @Test
  fun a_tap_beside_every_dot_falls_back_to_the_nearest_within_reach() {
    // 0.003° west: off every disk, but within tolerance of dot 0 only.
    assertEquals(0, hitTest(dots, WebMercator.x(-0.003), WebMercator.y(0.0), radius, tolerance))
  }

  @Test
  fun a_tap_away_from_every_dot_hits_nothing() {
    assertNull(hitTest(dots, WebMercator.x(0.01), WebMercator.y(0.0), radius, tolerance))
  }

  @Test
  fun stacked_dots_resolve_to_the_newest_the_one_drawn_on_top() {
    assertEquals(2, hitTest(dots, WebMercator.x(0.001), WebMercator.y(0.0), radius, tolerance))
  }
}
