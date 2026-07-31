package pl.llp.aircasting.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebMercatorTest {

  @Test
  fun `longitude spans the unit square`() {
    assertEquals(0.0, WebMercator.x(-180.0), 1e-12)
    assertEquals(0.5, WebMercator.x(0.0), 1e-12)
    assertEquals(1.0, WebMercator.x(180.0), 1e-12)
  }

  @Test
  fun `equator is the vertical midpoint and latitude grows downwards`() {
    assertEquals(0.5, WebMercator.y(0.0), 1e-12)
    assertTrue(WebMercator.y(40.0) < 0.5, "north of the equator must be nearer the top edge")
    assertTrue(WebMercator.y(-40.0) > 0.5, "south of the equator must be nearer the bottom edge")
  }

  @Test
  fun `poles are clamped inside the square`() {
    assertEquals(0.0, WebMercator.y(90.0), 1e-9)
    assertEquals(1.0, WebMercator.y(-90.0), 1e-9)
  }
}

class ScreenTransformTest {

  private val camera = MapCamera(centerLatitude = 40.7205, centerLongitude = -73.9865, zoom = 15f)

  @Test
  fun `camera centre lands at the viewport centre`() {
    val transform = ScreenTransform(camera, widthPx = 1080f, heightPx = 1920f, tileSizePx = 256f)

    assertEquals(540f, transform.screenX(WebMercator.x(camera.centerLongitude)), 1e-3f)
    assertEquals(960f, transform.screenY(WebMercator.y(camera.centerLatitude)), 1e-3f)
  }

  @Test
  fun `one zoom level doubles the pixel distance between two points`() {
    val at15 = ScreenTransform(camera, 1080f, 1920f, 256f)
    val at16 = ScreenTransform(camera.copy(zoom = 16f), 1080f, 1920f, 256f)

    val other = WebMercator.x(camera.centerLongitude + 0.001)
    val spread15 = at15.screenX(other) - 540f
    val spread16 = at16.screenX(other) - 540f

    assertEquals(2f, spread16 / spread15, 1e-3f)
  }

  @Test
  fun `at zoom zero the world is exactly one tile wide`() {
    val transform = ScreenTransform(
      MapCamera(0.0, 0.0, 0f), widthPx = 256f, heightPx = 256f, tileSizePx = 256f,
    )

    assertEquals(0f, transform.screenX(WebMercator.x(-180.0)), 1e-3f)
    assertEquals(256f, transform.screenX(WebMercator.x(180.0)), 1e-3f)
  }
}
