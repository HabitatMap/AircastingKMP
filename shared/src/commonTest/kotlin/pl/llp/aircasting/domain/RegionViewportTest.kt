package pl.llp.aircasting.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RegionViewportTest {

  @Test
  fun zoom_follows_how_much_longitude_fits_the_width() {
    // The whole world in 256 pt = zoom 0; 90° in 512 pt = 360·512 / (256·90) = 8 = 2³.
    assertEquals(0.0, regionViewport(0.0, 0.0, 170.0, 360.0, widthPoints = 256.0).zoom)
    assertEquals(3.0, regionViewport(0.0, 0.0, 60.0, 90.0, widthPoints = 512.0).zoom, 1e-9)
  }

  @Test
  fun bounds_are_centre_plus_minus_half_the_span() {
    val bounds = regionViewport(40.0, -74.0, latitudeDelta = 1.0, longitudeDelta = 2.0, widthPoints = 400.0).bounds

    assertEquals(GeoSquare(north = 40.5, south = 39.5, east = -73.0, west = -75.0), bounds)
  }

  @Test
  fun across_the_antimeridian_west_is_greater_than_east() {
    val bounds = regionViewport(0.0, 179.0, latitudeDelta = 2.0, longitudeDelta = 4.0, widthPoints = 400.0).bounds

    assertEquals(177.0, bounds.west)
    assertEquals(-179.0, bounds.east)
  }

  @Test
  fun a_zoomed_out_world_is_clamped_to_valid_bounds_and_zoom() {
    val viewport = regionViewport(10.0, 30.0, latitudeDelta = 200.0, longitudeDelta = 360.0, widthPoints = 100.0)

    assertEquals(GeoSquare(north = 90.0, south = -90.0, east = 180.0, west = -180.0), viewport.bounds)
    assertEquals(0.0, viewport.zoom) // log2(100/256) < 0 → 0, the API's minimum
  }
}