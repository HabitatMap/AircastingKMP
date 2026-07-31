package pl.llp.aircasting.map

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathSimplifierTest {

  @Test
  fun `collinear points collapse to the two endpoints`() {
    val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
    val y = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)

    assertContentEquals(intArrayOf(0, 4), simplifyPath(x, y, epsilon = 0.001))
  }

  @Test
  fun `a deviation larger than epsilon is kept`() {
    val x = doubleArrayOf(0.0, 1.0, 2.0)
    val y = doubleArrayOf(0.0, 1.0, 0.0)

    assertContentEquals(intArrayOf(0, 1, 2), simplifyPath(x, y, epsilon = 0.5))
  }

  @Test
  fun `a deviation smaller than epsilon is dropped`() {
    val x = doubleArrayOf(0.0, 1.0, 2.0)
    val y = doubleArrayOf(0.0, 0.1, 0.0)

    assertContentEquals(intArrayOf(0, 2), simplifyPath(x, y, epsilon = 0.5))
  }

  @Test
  fun `endpoints always survive and order is preserved`() {
    val track = generateTrack(pointCount = 5_000)
    val dots = track.project(pm25Threshold)

    val kept = simplifyPath(dots.worldX, dots.worldY, epsilon = 1.0 / (256.0 * 32768.0))

    assertEquals(0, kept.first())
    assertEquals(dots.size - 1, kept.last())
    assertTrue(kept.size < dots.size, "a jittery GPS track must compress")
    assertTrue(kept.toList() == kept.sorted(), "indices must stay ascending")
  }
}
