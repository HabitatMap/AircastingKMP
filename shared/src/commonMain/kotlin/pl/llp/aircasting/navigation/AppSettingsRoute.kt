package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import pl.llp.aircasting.settings.app.AppSetting
import pl.llp.aircasting.settings.app.AppSettingRow
import pl.llp.aircasting.settings.app.SettingsAppScreen

@Composable
fun AppSettingsRoute(onBack: () -> Unit) {
  val toggles = remember {
    mutableStateMapOf(
      AppSetting.CrowdMap to true,
      AppSetting.DisableMapping to false,
      AppSetting.DarkMode to false,
      AppSetting.PushNotifications to true,
      AppSetting.WifiOnlySync to false,
    )
  }
  val values = mapOf(
    AppSetting.TemperatureUnits to "°F",
    AppSetting.RegionalFormats to "US",
    AppSetting.Language to "English",
    AppSetting.MapType to "Default",
  )
  val rows = AppSetting.entries.map { setting ->
    toggles[setting]
      ?.let { AppSettingRow.Toggle(setting, it) }
      ?: AppSettingRow.Link(setting, values[setting])
  }
  SettingsAppScreen(
    rows = rows,
    onBack = onBack,
    onToggle = { setting, checked -> toggles[setting] = checked },
    onOpen = { Logger.withTag("AppSettings").d { "open $it" } },
  )
}