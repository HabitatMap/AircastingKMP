package pl.llp.aircasting.map

import kotlin.test.Test
import kotlin.test.assertEquals

class TopmostPerPixelTest {
  private fun run(xs: FloatArray, ys: FloatArray, margin: Float = 0f, cellSize: Float = 1f): List<Int> {
    val out = IntArray(xs.size)
    val n = topmostPerPixel(xs, ys, width = 100f, height = 100f, margin, cellSize, PixelCellSet(xs.size), out)
    return out.take(n)
  }

  @Test
  fun dots_in_the_same_pixel_keep_only_the_newest_listed_newest_first() {
    // 0 and 1 share pixel (10, 10); 2 has its own.
    assertEquals(listOf(2, 1), run(floatArrayOf(10f, 10.4f, 50f), floatArrayOf(10f, 10.2f, 10f)))
  }

  @Test
  fun dots_beyond_the_margin_are_culled() {
    assertEquals(listOf(1), run(floatArrayOf(-20f, 50f, 130f), floatArrayOf(50f, 50f, 50f), margin = 16f))
  }

  @Test
  fun dots_in_the_same_cell_keep_only_the_newest() {
    // Cell 10 px: 0 (x 10) and 1 (x 18) share cell [10, 20); 2 (x 25) is in the next one.
    assertEquals(listOf(2, 1), run(floatArrayOf(10f, 18f, 25f), floatArrayOf(5f, 5f, 5f), cellSize = 10f))
  }

  @Test
  fun cells_left_of_and_above_the_view_are_as_wide_as_any_other() {
    // -3 and 3 are in different 10 px cells. Truncating toward zero would merge
    // (-10, 10) into one double-width cell at the left/top edges.
    assertEquals(listOf(1, 0), run(floatArrayOf(-3f, 3f), floatArrayOf(5f, 5f), margin = 16f, cellSize = 10f))
  }
}
