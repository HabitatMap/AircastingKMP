package pl.llp.aircasting.map

import kotlin.test.Test
import kotlin.test.assertEquals

class TopmostPerPixelTest {
  private fun run(xs: FloatArray, ys: FloatArray, margin: Float = 0f): List<Int> {
    val out = IntArray(xs.size)
    val n = topmostPerPixel(xs, ys, width = 100f, height = 100f, margin, PixelCellSet(xs.size), out)
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
}
