package pl.llp.aircasting.settings.app

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingTest {

  @Test
  fun `settings are in design order`() {
    assertEquals(
      listOf(
        AppSetting.CrowdMap,
        AppSetting.DisableMapping,
        AppSetting.TemperatureUnits,
        AppSetting.RegionalFormats,
        AppSetting.Language,
        AppSetting.MapType,
        AppSetting.DarkMode,
        AppSetting.MicrophoneCalibration,
        AppSetting.PushNotifications,
        AppSetting.WifiOnlySync,
        AppSetting.CustomDataServer,
      ),
      AppSetting.entries,
    )
  }

  @Test
  fun `each section is contiguous and sections follow design order`() {
    assertEquals(
      AppSettingsSection.entries.toList(),
      AppSetting.entries.map { it.section }
        .distinct(),
    )
  }

  @Test
  fun `every section has a distinct non-blank header`() {
    val headers = AppSettingsSection.entries.map { EnStrings.header(it) }
    assertTrue(headers.none { it.isBlank() }, "blank header in $headers")
    assertEquals(headers.size, headers.toSet().size, "duplicate header in $headers")
  }

  @Test
  fun `every setting has a distinct non-blank label`() {
    val labels = AppSetting.entries.map { EnStrings.label(it) }
    assertTrue(labels.none { it.isBlank() }, "blank label in $labels")
    assertEquals(labels.size, labels.toSet().size, "duplicate label in $labels")
  }

  @Test
  fun `only the rows the design gives a supporting line have a subtitle`() {
    // Subtitle is nullable, so a forgotten branch is a silent layout change rather than a
    // compile error. Pinning the exact set makes the nulls deliberate.
    assertEquals(
      setOf(
        AppSetting.CrowdMap,
        AppSetting.DisableMapping,
        AppSetting.RegionalFormats,
        AppSetting.MicrophoneCalibration,
        AppSetting.PushNotifications,
        AppSetting.WifiOnlySync,
        AppSetting.CustomDataServer,
      ),
      AppSetting.entries.filter { EnStrings.subtitle(it) != null }
        .toSet(),
    )
    AppSetting.entries.forEach { setting ->
      EnStrings.subtitle(setting)
        ?.let {
          assertTrue(it.isNotBlank(), "blank subtitle for $setting")
        }
    }
  }
}