package pl.llp.aircasting.settings.app

import androidx.lifecycle.ViewModel
import cafe.adriel.lyricist.LanguageTag
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.StateFlow
import pl.llp.aircasting.settings.mic.CalibrationOffsetRange

class AppSettingsViewModel(private val repository: AppSettingsRepository) : ViewModel() {

  private val log = Logger.withTag("AppSettings")

  val preferences: StateFlow<AppPreferences> = repository.preferences

  fun toggle(setting: AppSetting, enabled: Boolean) = repository.update { prefs ->
    when (setting) {
      AppSetting.CrowdMap -> prefs.copy(isCrowdMapEnabled = enabled)
      // The row is the negative of the pref — see AppPreferences.isLocationTrackingEnabled.
      AppSetting.DisableMapping -> prefs.copy(isLocationTrackingEnabled = !enabled)
      // Writing a Boolean is what pins the theme: from here on the system is ignored.
      AppSetting.DarkMode -> prefs.copy(isDarkMode = enabled)
      AppSetting.PushNotifications -> prefs.copy(isPushNotificationsEnabled = enabled)
      AppSetting.WifiOnlySync -> prefs.copy(isWifiOnlySync = enabled)
      // Exhaustive and inert: these rows navigate, and a toggle reaching them is a wiring bug
      // worth a log rather than a silent write.
      AppSetting.TemperatureUnits,
      AppSetting.RegionalFormats,
      AppSetting.Language,
      AppSetting.MapType,
      AppSetting.MicrophoneCalibration,
      AppSetting.CustomDataServer -> prefs.also { log.w { "$setting is not a switch" } }
    }
  }
  fun choose(unit: TemperatureUnit) = repository.update { it.copy(temperatureUnit = unit) }

  fun choose(format: RegionalFormat) = repository.update { it.copy(regionalFormat = format) }

  fun choose(type: MapType) = repository.update { it.copy(mapType = type) }

  fun chooseLanguage(tag: LanguageTag) = repository.update { it.copy(language = tag) }

  fun chooseCalibrationOffset(offset: Int) = repository.update {
    it.copy(microphoneCalibrationOffset = offset.coerceIn(CalibrationOffsetRange))
  }
}
