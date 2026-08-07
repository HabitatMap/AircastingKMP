package pl.llp.aircasting.settings.mic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignalPowerTest {
  @Test
  fun `silence has no measurable power`() {
    assertNull(powerDbFs(ShortArray(4410)))
  }

  @Test
  fun `a constant DC offset is not sound`() {
    assertNull(powerDbFs(ShortArray(4410) { 12_345 }))
  }

  @Test
  fun `a saturated signal is rejected as faulty`() {
    assertNull(powerDbFs(square(amplitude = 32_767)))
  }
  @Test
  fun `dBFS is negative and halving the amplitude costs about six dB`() {
    val loud = powerDbFs(square(amplitude = 16_384))!!
    val quiet = powerDbFs(square(amplitude = 8_192))!!

    assertTrue(loud < 0.0, "full scale is 0 dBFS, so anything quieter is negative: was $loud")
    assertEquals(6.02, loud - quiet, absoluteTolerance = 0.01)
  }

  @Test
  fun `a half-scale square wave matches the legacy reading`() {
    assertEquals(-5.42, powerDbFs(square(amplitude = 16_384))!!, absoluteTolerance = 0.01)
  }
  @Test
  fun `only the filled prefix of a block is measured`() {
    val partlyFilled = ShortArray(4410)
    square(amplitude = 16_384, size = 100).copyInto(partlyFilled)

    assertEquals(
      powerDbFs(square(amplitude = 16_384, size = 100))!!,
      powerDbFs(partlyFilled, count = 100)!!,
      absoluteTolerance = 1e-9,
    )
  }

  private fun square(amplitude: Int, size: Int = 4410) =
    ShortArray(size) { (if (it % 2 == 0) amplitude else -amplitude).toShort() }
}