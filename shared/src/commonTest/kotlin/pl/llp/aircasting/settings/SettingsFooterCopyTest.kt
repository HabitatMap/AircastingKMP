package pl.llp.aircasting.settings

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsFooterCopyTest {

  @Test
  fun `version line interpolates the version it is given`() {
    assertEquals("AirCasting v3.2.1", EnStrings.settingsVersion("3.2.1"))
    assertEquals("AirCasting v1.0", EnStrings.settingsVersion("1.0"))
  }

  @Test
  fun `tagline matches the design copy`() {
    assertEquals("HabitatMap · Open source air quality", EnStrings.settingsTagline)
  }
}