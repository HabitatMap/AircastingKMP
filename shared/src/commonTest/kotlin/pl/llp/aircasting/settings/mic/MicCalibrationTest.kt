package pl.llp.aircasting.settings.mic

import kotlin.test.Test
import kotlin.test.assertEquals

class MicCalibrationTest {
  @Test
  fun `the offset is added to the raw dBFS reading`() {
    assertEquals(60.0, calibrate(dbFs = -40.0, offset = 100))
    assertEquals(0.0, calibrate(dbFs = -100.0, offset = 100))
  }
  @Test
  fun `calibration matches the legacy projection it replaces`() {
    // Legacy AirCasting projected [-(cal-60), 0] onto [60, cal]. Both ranges have width cal-60,
    // so the slope is 1 and the whole thing is "add the offset". This test is that proof: it
    // stops the simplification from silently drifting away from historical readings.
    fun legacyProjection(value: Double, calibration: Int): Double {
      val low = -(calibration - 60).toDouble()
      return 60 + (value - low) / (0 - low) * (calibration - 60)
    }

    for (offset in listOf(61, 80, 90, 100, 140)) {
      for (raw in listOf(-90.0, -55.5, -40.0, -12.0, -0.5)) {
        assertEquals(
          legacyProjection(raw, offset),
          calibrate(raw, offset),
          absoluteTolerance = 1e-9,
          message = "raw=$raw offset=$offset",
        )
      }
    }
  }
  @Test
  fun `stepping cannot leave the range the stepper offers`() {
    assertEquals(101, 100.steppedBy(1))
    assertEquals(99, 100.steppedBy(-1))
    assertEquals(CalibrationOffsetRange.last, CalibrationOffsetRange.last.steppedBy(1))
    assertEquals(CalibrationOffsetRange.first, CalibrationOffsetRange.first.steppedBy(-1))
  }

  @Test
  fun `the default offset is the one the design resets to`() {
    // The reset button's label interpolates this, so the copy and the behaviour cannot disagree.
    assertEquals(100, DefaultCalibrationOffset)
  }
  @Test
  fun `waveform loudness spans the legacy alert thresholds`() {
    assertEquals(0f, loudnessFraction(20.0))
    assertEquals(0.5f, loudnessFraction(60.0))
    assertEquals(1f, loudnessFraction(100.0))
    assertEquals(0f, loudnessFraction(-30.0), "an uncalibrated reading must not go negative")
    assertEquals(1f, loudnessFraction(130.0), "a loud reading must not overflow the bars")
  }

  @Test
  fun `a reading is shown to one decimal place`() {
    assertEquals("78.8", formatReading(78.7649))
    assertEquals("100.0", formatReading(100.0))
  }
}