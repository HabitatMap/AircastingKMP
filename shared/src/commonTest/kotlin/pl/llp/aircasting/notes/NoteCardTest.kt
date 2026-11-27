package pl.llp.aircasting.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Figma note card meta (1253:399): "PM 2.5: 8 µg/m³" for the selected stream. */
class NoteCardTest {
  @Test
  fun `value label names the stream like the details chip and rounds the reading`() {
    assertEquals("PM 2.5: 8 µg/m³", noteValueLabel("AirBeam-PM2.5", 7.6, "µg/m³"))
  }

  @Test
  fun `value label works for non PM streams`() {
    assertEquals("Phone Microphone: 52 dB", noteValueLabel("Phone Microphone", 52.4, "dB"))
  }

  @Test
  fun `no reading on the stream by the note time means no label`() {
    assertNull(noteValueLabel("AirBeam-PM2.5", null, "µg/m³"))
  }
}
