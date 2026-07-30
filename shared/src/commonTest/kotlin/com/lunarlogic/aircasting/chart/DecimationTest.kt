package com.lunarlogic.aircasting.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecimationTest {

  private fun ramp(n: Int) = List(n) { ChartPoint(x = it.toDouble(), y = it.toDouble()) }

  @Test
  fun `returns input untouched when threshold is not smaller than size`() {
    val points = ramp(10)
    assertEquals(points, lttb(points, threshold = 10))
    assertEquals(points, lttb(points, threshold = 99))
  }

  @Test
  fun `emits exactly threshold points`() {
    assertEquals(500, lttb(ramp(100_000), threshold = 500).size)
  }

  @Test
  fun `keeps first and last point`() {
    val points = ramp(10_000)
    val out = lttb(points, threshold = 100)
    assertEquals(points.first(), out.first())
    assertEquals(points.last(), out.last())
  }

  @Test
  fun `preserves x order`() {
    val out = lttb(ramp(50_000), threshold = 300)
    assertTrue(out.zipWithNext().all { (a, b) -> a.x < b.x })
  }

  @Test
  fun `keeps an isolated spike that naive sampling would drop`() {
    // Flat line with one tall spike at an index no every-Nth sampler would land on.
    val points = MutableList(10_000) { ChartPoint(it.toDouble(), 1.0) }
    points[3_333] = ChartPoint(3_333.0, 999.0)

    val out = lttb(points, threshold = 200)

    assertTrue(out.any { it.y == 999.0 }, "LTTB must retain the spike")
  }

  @Test
  fun `handles degenerate inputs`() {
    assertEquals(emptyList(), lttb(emptyList(), threshold = 100))
    assertEquals(listOf(ChartPoint(0.0, 0.0)), lttb(ramp(1), threshold = 1))
    assertEquals(2, lttb(ramp(100), threshold = 2).size)
  }
}
