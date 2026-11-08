package pl.llp.aircasting.active

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ActiveCardFormattingTest {
  private val now = Instant.fromEpochMilliseconds(1_786_000_000_000)

  @Test
  fun temperature_and_humidity_chips_read_as_words() {
    assertEquals("Temp.", chipLabel("AirBeam3-F", EnStrings))
    assertEquals("Hum.", chipLabel("AirBeam3-RH", EnStrings))
  }

  @Test
  fun pm_chips_read_as_in_explore() {
    assertEquals("PM 2.5", chipLabel("AirBeamMini-PM2.5", EnStrings))
    assertEquals("PM 10", chipLabel("AirBeam3-PM10", EnStrings))
  }

  @Test
  fun values_are_whole_numbers_and_a_missing_one_is_two_dashes() {
    assertEquals("12", valueText(12.4))
    assertEquals("13", valueText(12.6))
    assertEquals("--", valueText(null))
  }

  @Test
  fun meta_names_the_device_then_how_long_ago_it_reported() {
    assertEquals(listOf("AirBeam Mini", "1 min ago"), card(lastSeen = now - 90.seconds).meta(now, EnStrings))
  }

  @Test
  fun meta_has_no_age_before_the_first_reading() {
    assertEquals(listOf("AirBeam Mini"), card(lastSeen = null).meta(now, EnStrings))
  }

  private fun card(lastSeen: Instant?) = ActiveSessionCard(
    uuid = "s1",
    name = "Living room",
    tags = emptyList(),
    deviceModel = "AirBeamMini",
    state = if (lastSeen == null) CardState.NoDataYet else CardState.Live,
    lastSeen = lastSeen,
    streams = emptyList(),
    selected = null,
  )
}
