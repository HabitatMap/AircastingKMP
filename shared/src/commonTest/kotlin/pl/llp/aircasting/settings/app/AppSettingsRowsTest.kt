package pl.llp.aircasting.settings.app

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppSettingsRowsTest {
  @Test
  fun `every setting gets exactly one row, in design order`() {
    val rows = AppPreferences().toRows(EnStrings, systemDarkMode = false)
    assertEquals(AppSetting.entries, rows.map { it.setting })
  }

  @Test
  fun `switch rows read the stored flags`() {
    val rows = AppPreferences(
      crowdMapEnabled = false,
      pushNotificationsEnabled = false,
      wifiOnlySync = true,
    ).toRows(EnStrings, systemDarkMode = false)
      .associateBy { it.setting }
    assertEquals(AppSettingRow.Toggle(AppSetting.CrowdMap, false), rows[AppSetting.CrowdMap])
    assertEquals(
      AppSettingRow.Toggle(AppSetting.PushNotifications, false),
      rows[AppSetting.PushNotifications]
    )
    assertEquals(AppSettingRow.Toggle(AppSetting.WifiOnlySync, true), rows[AppSetting.WifiOnlySync])
  }

  @Test
  fun `Disable mapping shows the inverse of the stored flag`() {
    // The pref is stored positive (locationTrackingEnabled); the row is phrased as a negative.
    // Getting this backwards silently turns location recording off for everyone, so it is the
    // single most important assertion on this screen.
    fun checked(tracking: Boolean) =
      AppPreferences(locationTrackingEnabled = tracking)
        .toRows(EnStrings, systemDarkMode = false)
        .filterIsInstance<AppSettingRow.Toggle>()
        .single { it.setting == AppSetting.DisableMapping }
        .checked

    assertEquals(false, checked(tracking = true))
    assertEquals(true, checked(tracking = false))
  }

  @Test
  fun `dark mode follows the system until the user pins it`() {
    fun checked(stored: Boolean?, system: Boolean) =
      AppPreferences(darkMode = stored)
        .toRows(EnStrings, systemDarkMode = system)
        .filterIsInstance<AppSettingRow.Toggle>()
        .single { it.setting == AppSetting.DarkMode }
        .checked

    assertEquals(true, checked(stored = null, system = true), "unset must mirror the system")
    assertEquals(false, checked(stored = null, system = false))
    assertEquals(false, checked(stored = false, system = true), "an explicit choice wins")
    assertEquals(true, checked(stored = true, system = false))
  }

  @Test
  fun `link rows show the formatted preference`() {
    val rows = AppPreferences(
      temperatureUnit = TemperatureUnit.Celsius,
      regionalFormat = RegionalFormat.Metric,
      mapType = MapType.Satellite,
    ).toRows(EnStrings, systemDarkMode = false)
      .associateBy { it.setting }

    assertEquals(
      AppSettingRow.Link(AppSetting.TemperatureUnits, "°C"),
      rows[AppSetting.TemperatureUnits]
    )
    assertEquals(
      AppSettingRow.Link(AppSetting.RegionalFormats, "Metric"),
      rows[AppSetting.RegionalFormats]
    )
    assertEquals(AppSettingRow.Link(AppSetting.MapType, "Satellite"), rows[AppSetting.MapType])
    // Language is not a stored pref yet — it reports the locale whose copy is loaded.
    assertEquals(AppSettingRow.Link(AppSetting.Language, "English"), rows[AppSetting.Language])
  }

  @Test
  fun `rows that only navigate carry no value`() {
    val rows = AppPreferences().toRows(EnStrings, systemDarkMode = false)
      .associateBy { it.setting }
    assertNull((rows[AppSetting.MicrophoneCalibration] as AppSettingRow.Link).value)
    assertNull((rows[AppSetting.CustomDataServer] as AppSettingRow.Link).value)
  }
}