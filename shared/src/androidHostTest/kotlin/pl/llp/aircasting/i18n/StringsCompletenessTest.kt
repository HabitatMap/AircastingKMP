package pl.llp.aircasting.i18n

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards every locale in [AppStrings] against blank or forgotten copy.
 *
 * Lives in `androidHostTest` rather than `commonTest` because enumerating a data class's
 * properties needs full reflection, which `commonMain`'s `kotlin.reflect` does not offer — the
 * JVM test target does. The alternative (hand-listing ~100 accessors) would rot instantly.
 */
class StringsCompletenessTest {
  /** Copy that is legitimately identical in every locale: symbols, brand and chemical names. */
  private val sharedAcrossLocales = setOf(
    "appLogo",
    "appSettingValueFahrenheit",
    "appSettingValueCelsius",
    "appSettingValueRegionUs",
    "appSettingValueMapSatellite",
    "appSettingsNotificationsHeader",
    "pollutantNo2",
    "pollutantOzone",
    "appSettingFahrenheit",
    "appSettingCelsius",
    "micDecibels",
    "customServerUrlLabel",
    "customServerPortLabel"
  )

  /** Only the plain `String` fields; the parameterised ones are lambdas and can't be compared. */
  private fun textOf(strings: Strings): Map<String, String> =
    Strings::class.java.declaredFields
      .filter { it.type == String::class.java }
      .associate { field ->
        field.isAccessible = true
        field.name to field.get(strings) as String
      }

  @Test
  fun `reflection actually sees the copy`() {
    assertTrue(textOf(EnStrings).size > 50, "Only found ${textOf(EnStrings).size} text fields")
  }

  @Test
  fun `no locale ships blank copy`() {
    AppStrings.forEach { (tag, strings) ->
      textOf(strings).forEach { (name, value) ->
        assertTrue(value.isNotBlank(), "$tag.$name is blank")
      }
    }
  }

  @Test
  fun `French copy is actually translated`() {
    val english = textOf(EnStrings)
    val untranslated = textOf(FrStrings)
      .filterKeys { it !in sharedAcrossLocales }
      .filter { (name, value) -> english[name] == value }
      .keys
    assertTrue(untranslated.isEmpty(), "Still English in FrStrings: $untranslated")
  }
}
