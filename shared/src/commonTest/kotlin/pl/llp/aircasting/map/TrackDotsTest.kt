package pl.llp.aircasting.map

import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals


class TrackDotsTest {
  private val pm25 = SensorThreshold("AirBeam-PM2.5", veryLow = 0, low = 12, medium = 35, high = 55, veryHigh = 150)

  @Test
  fun each_point_is_projected_and_coloured_by_the_threshold() {
    val dots = TrackDots.of(
      listOf(TrackPoint(GeoLocation(0.0, 0.0), 5.0), TrackPoint(GeoLocation(0.0, 90.0), 60.0)),
      pm25,
    )

    assertEquals(0.5, dots.worldX[0], 1e-12)
    assertEquals(0.75, dots.worldX[1], 1e-12)
    assertEquals(0.5, dots.worldY[0], 1e-12)
    assertEquals(MeasurementLevel.LOW.ordinal, dots.level[0])
    assertEquals(MeasurementLevel.VERY_HIGH.ordinal, dots.level[1])
  }

  @Test
  fun no_threshold_or_no_reading_leaves_the_dot_uncoloured() {
    val here = GeoLocation(52.0, 21.0)

    assertEquals(TrackDots.Uncoloured, TrackDots.of(listOf(TrackPoint(here, 5.0)), threshold = null).level[0])
    assertEquals(TrackDots.Uncoloured, TrackDots.of(listOf(TrackPoint(here, null)), pm25).level[0])
  }
}