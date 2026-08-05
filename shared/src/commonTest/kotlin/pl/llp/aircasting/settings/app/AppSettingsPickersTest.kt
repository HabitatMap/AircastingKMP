package pl.llp.aircasting.settings.app

import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.EnStrings
import pl.llp.aircasting.i18n.FrStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsPickersTest {

  @Test
  fun `temperature options follow the sheet order, each with a worked example`() {
    assertEquals(
      listOf(
        ChoiceOption(TemperatureUnit.Fahrenheit, "Fahrenheit", "°F • eg. 72°F"),
        ChoiceOption(TemperatureUnit.Celsius, "Celsius", "°C • eg. 22°C"),
      ),
      EnStrings.temperatureOptions(),
    )
  }

  @Test
  fun `map options follow the sheet order and carry no supporting line`() {
    assertEquals(
      listOf(
        ChoiceOption(MapType.Default, "Default"),
        ChoiceOption(MapType.Satellite, "Satellite"),
      ),
      EnStrings.mapTypeOptions(),
    )
  }

  @Test
  fun `every regional option covers a preset, in enum order`() {
    assertEquals(RegionalFormat.entries, EnStrings.regionalFormatOptions().map { it.value })
  }

  @Test
  fun `each regional preset previews its own separators and distance unit`() {
    // The whole reason RegionalFormat carries its separators: this line is computed from them,
    // so a preset can never advertise a format it does not produce.
    assertEquals(
      listOf(
        "US format" to "MM/DD/YYYY • 1,234.5 • Miles",
        "UK format" to "DD/MM/YYYY • 1,234.5 • Miles",
        "Central European" to "DD.MM.YYYY • 1.234,5 • Kilometers",
        "Nordic" to "DD.MM.YYYY • 1 234,5 • Kilometers",
        "East Asian" to "YYYY/MM/DD • 1,234.5 • Kilometers",
      ),
      EnStrings.regionalFormatOptions().map { it.label to it.supporting },
    )
  }

  @Test
  fun `the language sheet only offers locales that are actually translated`() {
    // Derived from AppStrings, so a new locale shows up in the sheet the moment its copy lands
    // — and an untranslated language can never be selected into English-in-disguise.
    assertEquals(
      listOf(
        ChoiceOption("en", EnStrings.languageName),
        ChoiceOption("fr", FrStrings.languageName),
      ),
      languageOptions(AppStrings),
    )
  }

  @Test
  fun `a stored language wins over the system one`() {
    assertEquals("fr", resolveLanguage(stored = "fr", system = "en-US", available = AppStrings.keys))
  }

  @Test
  fun `a regional system tag selects its bare language`() {
    // Lyricist strips the region on its second lookup, so "fr-CA" is served FrStrings. The
    // radio must land on the row Lyricist actually picked, not on nothing.
    assertEquals("fr", resolveLanguage(stored = null, system = "fr-CA", available = AppStrings.keys))
  }

  @Test
  fun `an untranslated system locale resolves to English`() {
    assertEquals("en", resolveLanguage(stored = null, system = "de-DE", available = AppStrings.keys))
  }

  @Test
  fun `a stored language that is no longer shipped falls back instead of selecting nothing`() {
    assertEquals("en", resolveLanguage(stored = "es", system = "en-US", available = AppStrings.keys))
  }
}