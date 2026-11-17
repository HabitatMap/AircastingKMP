package pl.llp.aircasting.map

import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FitCameraTest {
  @Test
  fun the_inverse_projection_round_trips() {
    assertEquals(52.2297, WebMercator.latitude(WebMercator.y(52.2297)), 1e-9)
    assertEquals(21.0122, WebMercator.longitude(WebMercator.x(21.0122)), 1e-9)
  }

  @Test
  fun nothing_to_frame_gives_no_camera() {
    assertNull(fitCamera(emptyList(), widthDp = 400f, heightDp = 400f))
  }
  @Test
  fun a_single_point_is_centred_at_street_level() {
    val camera = fitCamera(listOf(GeoLocation(40.7, -74.0)), 400f, 400f)!!

    assertEquals(40.7, camera.centerLatitude, 1e-9)
    assertEquals(-74.0, camera.centerLongitude, 1e-9)
    assertEquals(MaxFitZoom, camera.zoom)
  }

  @Test
  fun a_track_is_centred_and_its_longer_side_touches_the_padding() {
    // 0.01° each way at 40°N: Mercator stretches latitude by 1/cos(40°), so height is the tight axis.
    val camera = fitCamera(
      listOf(GeoLocation(40.0, -74.0), GeoLocation(40.01, -73.99)),
      widthDp = 400f, heightDp = 400f, paddingDp = 32f,
    )!!
    // Same dp space fitCamera works in: 256 dp tiles.
    val screen = ScreenTransform(camera, widthPx = 400f, heightPx = 400f, tileSizePx = 256f)
    assertEquals(32f, screen.screenY(WebMercator.y(40.01)), 0.5f)
    assertEquals(368f, screen.screenY(WebMercator.y(40.0)), 0.5f)
    assertTrue(screen.screenX(WebMercator.x(-74.0)) >= 32f)
    assertTrue(screen.screenX(WebMercator.x(-73.99)) <= 368f)
  }

  @Test
  fun a_tiny_track_does_not_zoom_past_street_level() {
    val camera = fitCamera(listOf(GeoLocation(40.0, -74.0), GeoLocation(40.000001, -74.0)), 400f, 400f)!!

    assertEquals(MaxFitZoom, camera.zoom)
  }
}