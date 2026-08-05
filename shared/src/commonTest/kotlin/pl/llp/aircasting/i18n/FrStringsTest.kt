package pl.llp.aircasting.i18n

import cafe.adriel.lyricist.Lyricist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FrStringsTest {
  private fun lyricistAt(tag: String) =
    Lyricist("en", AppStrings).also { it.languageTag = tag }

  @Test
  fun `French is registered under the bare language tag`() {
    assertSame(FrStrings, AppStrings["fr"])
  }

  @Test
  fun `regional French locales resolve to French`() {
    // The system hands us "fr-FR" / "fr-CA"; Lyricist strips the region before the second lookup,
    // which is why registering plain "fr" is enough. This test pins that behaviour down.
    assertSame(FrStrings, lyricistAt("fr-FR").strings)
    assertSame(FrStrings, lyricistAt("fr-CA").strings)
  }

  @Test
  fun `unsupported locales still fall back to English`() {
    assertSame(EnStrings, lyricistAt("de-DE").strings)
  }

  @Test
  fun `the language row shows the locale's own name`() {
    assertEquals("Français", FrStrings.languageName)
  }

  @Test
  fun `parameterised copy is translated and not merely interpolated`() {
    assertEquals("AirCasting v1.2.3", FrStrings.settingsVersion("1.2.3"))
    assertEquals("à l'instant", FrStrings.ageJustNow)
    assertEquals("il y a 3 min", FrStrings.ageMinutesAgo(3))
    assertEquals("il y a 2 h", FrStrings.ageHoursAgo(2))
    assertEquals("il y a 4 j", FrStrings.ageDaysAgo(4))
    assertEquals("à 1.2 mi", FrStrings.distanceAway(1.2))
    assertEquals(
      "Saisissez le code à 4 chiffres envoyé à a@b.com. Il expire dans 30 minutes.",
      FrStrings.deleteAccountCodeBody("a@b.com"),
    )
  }
}
