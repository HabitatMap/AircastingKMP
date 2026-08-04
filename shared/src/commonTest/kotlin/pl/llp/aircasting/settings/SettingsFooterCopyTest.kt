package pl.llp.aircasting.settings

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test

class SettingsFooterCopyTest {

  @Test
  fun `version line interpolates the version it is given`() {
    // The whole point of making this a lambda: a template that ignored its argument would ship
    // a frozen version number that silently diverges from the build. Two different inputs must
    // produce two different lines, each containing what it was handed.
    assertEquals("AirCasting v3.2.1", EnStrings.settingsVersion("3.2.1"))
    assertEquals("AirCasting v1.0", EnStrings.settingsVersion("1.0"))
  }

  @Test
  fun `tagline matches the design copy`() {
    // Figma 161:34845. The separator is U+00B7 MIDDLE DOT — not a hyphen, not a bullet. Easy
    // to mangle on a copy-paste, and the mangling is visible on screen.
    assertEquals("HabitatMap · Open source air quality", EnStrings.settingsTagline)
  }
}