package pl.llp.aircasting.map

import kotlin.test.Test
import kotlin.test.assertEquals

class PathVerticesTest {
  /** "M3" = moveTo point 3, "L4" = lineTo point 4. View is 100 × 100. */
  private fun run(xs: FloatArray, ys: FloatArray, minStep: Float = 1f, margin: Float = 0f): List<String> {
    val out = mutableListOf<String>()
    forEachPathVertex(xs, ys, width = 100f, height = 100f, margin, minStep) { i, moveTo ->
      out += (if (moveTo) "M" else "L") + i
    }
    return out
  }

  @Test
  fun vertices_closer_than_min_step_to_the_last_drawn_one_are_skipped() {
    // 1 is 2 px from 0, 3 is 1 px from 2: both skipped at minStep 5.
    assertEquals(
      listOf("M0", "L2"),
      run(floatArrayOf(10f, 12f, 20f, 21f), floatArrayOf(50f, 50f, 50f, 50f), minStep = 5f),
    )
  }

  @Test
  fun a_segment_beyond_one_edge_is_dropped_and_the_path_resumes_with_a_move() {
    // 1 → 2 lies entirely right of the view; 0 → 1 and 2 → 3 cross into it.
    assertEquals(
      listOf("M0", "L1", "M2", "L3"),
      run(floatArrayOf(50f, 150f, 160f, 50f), floatArrayOf(50f, 50f, 60f, 60f)),
    )
  }

  @Test
  fun a_segment_crossing_the_view_is_kept_though_both_ends_are_outside() {
    assertEquals(listOf("M0", "L1"), run(floatArrayOf(-50f, 150f), floatArrayOf(50f, 50f)))
  }
}
