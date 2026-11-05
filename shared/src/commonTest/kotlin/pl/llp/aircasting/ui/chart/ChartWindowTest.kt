package pl.llp.aircasting.ui.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChartWindowTest {
  /** One point per second, x = 0 … n-1. */
  private fun seconds(n: Int) = List(n) { ChartPoint(it.toDouble(), 1.0) }

  @Test
  fun the_window_is_the_visible_range_plus_half_its_width_on_each_side() {
    assertEquals(50.0..250.0, windowFor(100.0..200.0))
  }

  @Test
  fun keeps_only_the_points_in_range_plus_one_neighbour_each_side() {
    // The neighbours let the line run off the edge instead of stopping short of it.
    val slice = pointsIn(seconds(1_000), 10.5..20.5)

    assertEquals(10.0, slice.first().x)
    assertEquals(21.0, slice.last().x)
  }

  @Test
  fun a_range_past_either_end_is_clamped_to_the_data() {
    val points = seconds(100)

    assertEquals(points, pointsIn(points, -50.0..500.0))
  }

  @Test
  fun a_gap_wider_than_the_range_still_gives_the_two_points_around_it() {
    val points = listOf(ChartPoint(0.0, 1.0), ChartPoint(1_000.0, 2.0))

    assertEquals(points, pointsIn(points, 400.0..600.0))
  }

  @Test
  fun a_full_day_at_1_Hz_is_cut_down_to_the_budget() {
    val day = seconds(86_400)

    assertEquals(POINTS_PER_WINDOW, pointsToDraw(day, 0.0..86_400.0).size)
  }

  @Test
  fun a_sparse_window_is_drawn_raw() {
    val hourly = List(24) { ChartPoint(it * 3_600.0, it.toDouble()) }

    assertEquals(hourly, pointsToDraw(hourly, 0.0..86_400.0))
  }

  @Test
  fun panning_inside_the_loaded_window_needs_no_reload() {
    assertFalse(needsReload(loaded = 50.0..250.0, visible = 120.0..220.0))
  }

  @Test
  fun panning_past_the_loaded_window_reloads() {
    assertTrue(needsReload(loaded = 50.0..250.0, visible = 200.0..300.0))
  }

  @Test
  fun zooming_in_past_twice_the_loaded_resolution_reloads() {
    // Loaded for a 100 s view; a 40 s view would show under half the points it was decimated for.
    assertTrue(needsReload(loaded = 50.0..250.0, visible = 130.0..170.0))
    assertFalse(needsReload(loaded = 50.0..250.0, visible = 120.0..180.0))
  }

  @Test
  fun the_window_at_max_zoom_on_1_Hz_data_is_drawn_raw() {
    // Legacy showed every measurement when zoomed all the way in; we keep that.
    val day = seconds(86_400)
    val window = windowFor(43_200.0..(43_200.0 + MIN_VISIBLE_SECONDS))

    assertEquals(pointsIn(day, window).size, pointsToDraw(day, window).size)
  }
}