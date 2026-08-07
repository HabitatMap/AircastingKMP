package pl.llp.aircasting.settings.app

import pl.llp.aircasting.data.network.DefaultBackendUrl
import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsRowsTest {
  @Test
  fun `the custom data server row shows the host in use, scheme stripped`() {
    val rows = AppPreferences(dataServerUrl = "https://my.server:8080").toRows(EnStrings, false)

    assertEquals(
      AppSettingRow.Link(AppSetting.CustomDataServer, "my.server:8080"),
      rows.single { it.setting == AppSetting.CustomDataServer },
    )
  }
  @Test
  fun `with no custom server the row says aircasting dot org`() {
    val rows = AppPreferences().toRows(EnStrings, false)

    assertEquals(
      AppSettingRow.Link(AppSetting.CustomDataServer, "aircasting.org"),
      rows.single { it.setting == AppSetting.CustomDataServer },
    )
  }
  @Test
  fun `every setting gets exactly one row in design order`() {
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
      regionalFormat = RegionalFormat.US,
      mapType = MapType.Satellite,
    ).toRows(EnStrings, systemDarkMode = false)
      .associateBy { it.setting }

    assertEquals(
      AppSettingRow.Link(AppSetting.TemperatureUnits, "°C"),
      rows[AppSetting.TemperatureUnits]
    )
    assertEquals(
      AppSettingRow.Link(AppSetting.RegionalFormats, "US format"),
      rows[AppSetting.RegionalFormats]
    )
    assertEquals(AppSettingRow.Link(AppSetting.MapType, "Satellite"), rows[AppSetting.MapType])
    assertEquals(AppSettingRow.Link(AppSetting.Language, "English"), rows[AppSetting.Language])
  }

  @Test
  fun `the data server row shows the server in use, official by default`() {
    fun value(url: String?) =
      AppPreferences(dataServerUrl = url)
        .toRows(EnStrings, systemDarkMode = false)
        .filterIsInstance<AppSettingRow.Link>()
        .single { it.setting == AppSetting.CustomDataServer }
        .value

    assertEquals("aircasting.org", value(null))
    assertEquals("my.server:8080", value("https://my.server:8080"))
  }

  @Test
  fun `microphone calibration shows the stored offset`() {
    val rows = AppPreferences(microphoneCalibrationOffset = 92)
      .toRows(EnStrings, systemDarkMode = false)
      .associateBy { it.setting }

    assertEquals(
      AppSettingRow.Link(AppSetting.MicrophoneCalibration, "92"),
      rows[AppSetting.MicrophoneCalibration],
    )
  }
}