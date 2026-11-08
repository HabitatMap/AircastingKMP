package pl.llp.aircasting.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamFormattingTest {

  @Test
  fun pm_chip_labels_get_a_space_before_the_size() {
    assertEquals("PM 1", streamChipLabel("AirBeam-PM1"))
    assertEquals("PM 2.5", streamChipLabel("AirBeamMini-PM2.5")) // raw spelling, in case BE sends it
    assertEquals("PM 10", streamChipLabel("AirBeam-PM10"))
    assertEquals("PM 2.5", streamChipLabel("Government-PM2.5"))
  }

  @Test
  fun other_chip_labels_are_just_the_sensor_suffix() {
    assertEquals("NO2", streamChipLabel("Government-NO2"))
    assertEquals("Ozone", streamChipLabel("Government-Ozone"))
  }

  @Test
  fun the_mini_model_id_reads_as_its_product_name() {
    assertEquals("AirBeam Mini", deviceDisplayName("AirBeamMini"))
    assertEquals("AirBeam3", deviceDisplayName("AirBeam3"))
  }
}
