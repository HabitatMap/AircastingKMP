package pl.llp.aircasting.settings.app

import cafe.adriel.lyricist.LanguageTag
import pl.llp.aircasting.i18n.Strings

data class ChoiceOption<T>(val value: T, val label: String, val supporting: String? = null)

fun Strings.temperatureOptions(): List<ChoiceOption<TemperatureUnit>> =
  TemperatureUnit.entries.map { ChoiceOption(it, name(it), example(it)) }
fun Strings.mapTypeOptions(): List<ChoiceOption<MapType>> =
  MapType.entries.map { ChoiceOption(it, display(it)) }

fun Strings.regionalFormatOptions(): List<ChoiceOption<RegionalFormat>> =
  RegionalFormat.entries.map { ChoiceOption(it, display(it), preview(it)) }

/** "MM/DD/YYYY • 1,234.5 • Miles" — assembled from the preset, not written per locale. */
fun Strings.preview(format: RegionalFormat): String =
  "${format.datePattern} • 1${format.groupingSeparator}234${format.decimalSeparator}5 • " +
    display(format.distanceUnit)

/**
 * The languages the sheet may offer: exactly those with copy, labelled in their own language.
 * Derived from [pl.llp.aircasting.i18n.AppStrings], so adding a locale adds a row for free.
 */
fun languageOptions(translations: Map<LanguageTag, Strings>): List<ChoiceOption<LanguageTag>> =
  translations.map { (tag, strings) -> ChoiceOption(tag, strings.languageName) }

/**
 * Which row the language radio sits on — mirroring Lyricist's own lookup order so the sheet
 * agrees with the copy that is actually on screen: the stored choice, else the system locale,
 * else the system locale with its region stripped ("fr-CA" -> "fr"), else Lyricist's default.
 */
fun resolveLanguage(
  stored: LanguageTag?,
  system: LanguageTag,
  available: Set<LanguageTag>,
  fallback: LanguageTag = "en",
): LanguageTag =
  listOfNotNull(stored, system, system.substringBefore('-').substringBefore('_'))
    .firstOrNull { it in available }
    ?: fallback

// Private `when`s rather than a listOf() of options: the compiler then refuses a new
// TemperatureUnit until it has both a name and an example.
private fun Strings.name(unit: TemperatureUnit) = when (unit) {
  TemperatureUnit.Fahrenheit -> appSettingFahrenheit
  TemperatureUnit.Celsius -> appSettingCelsius
}

private fun Strings.example(unit: TemperatureUnit) = when (unit) {
  TemperatureUnit.Fahrenheit -> appSettingFahrenheitDetail
  TemperatureUnit.Celsius -> appSettingCelsiusDetail
}