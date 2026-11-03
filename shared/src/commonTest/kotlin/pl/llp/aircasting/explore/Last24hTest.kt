package pl.llp.aircasting.explore

import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class Last24hTest {

  private val day = 86_400.0

  @Test
  fun points_come_out_sorted_whatever_order_they_arrived_in() {
    // The spec says newest first, live data comes oldest first: trust neither.
    val shuffled = listOf(ChartPoint(30.0, 3.0), ChartPoint(10.0, 1.0), ChartPoint(20.0, 2.0))

    assertEquals(listOf(10.0, 20.0, 30.0), last24h(shuffled).map { it.x })
  }
  @Test
  fun keeps_24_h_back_from_the_newest_point_boundary_included() {
    val newest = 10 * day
    val points = listOf(
      ChartPoint(newest - day - 1, 0.0), // just outside
      ChartPoint(newest - day, 1.0), // exactly 24 h before: kept, as legacy's `>=`
      ChartPoint(newest, 2.0),
    )

    assertEquals(listOf(newest - day, newest), last24h(points).map { it.x })
  }

  @Test
  fun a_station_that_went_quiet_still_shows_its_last_24_h() {
    // Anchored on the newest point, not on "now": no clock is involved at all.
    val old = List(48) { ChartPoint(it * 3_600.0, 1.0) }

    assertEquals(25, last24h(old).size) // hours 23…47
  }

  @Test
  fun no_points_gives_no_points() {
    assertEquals(emptyList(), last24h(emptyList()))
  }
}