package pl.llp.aircasting.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SingleChoiceSheet
import pl.llp.aircasting.settings.app.AppSetting
import pl.llp.aircasting.settings.app.AppSettingsViewModel
import pl.llp.aircasting.settings.app.SettingsAppScreen
import pl.llp.aircasting.settings.app.languageOptions
import pl.llp.aircasting.settings.app.mapTypeOptions
import pl.llp.aircasting.settings.app.regionalFormatOptions
import pl.llp.aircasting.settings.app.resolveLanguage
import pl.llp.aircasting.settings.app.temperatureOptions
import pl.llp.aircasting.settings.app.toRows
import pl.llp.aircasting.settings.mic.MicCalibrationSheet

@Composable
fun AppSettingsRoute(onBack: () -> Unit, onOpenCustomServer: () -> Unit) {
  val vm = koinViewModel<AppSettingsViewModel>()
  val preferences by vm.preferences.collectAsStateWithLifecycle()
  val strings = LocalStrings.current
  val systemDarkMode = isSystemInDarkTheme()
  val rows = remember(preferences, strings, systemDarkMode) {
    preferences.toRows(strings, systemDarkMode)
  }
  // Which sheet is open is pure UI state: it is not worth a ViewModel field, and it should not
  // survive process death — a half-made choice is better dropped than restored.
  var picker by remember { mutableStateOf<AppSetting?>(null) }
  val dismiss: () -> Unit = { picker = null }

  SettingsAppScreen(
    rows = rows,
    onBack = onBack,
    onToggle = vm::toggle,
    onOpen = { setting ->
      if (setting == AppSetting.CustomDataServer)
        onOpenCustomServer()
      else
        picker = setting
    },
  )
  when (picker) {
    AppSetting.TemperatureUnits -> SingleChoiceSheet(
      title = strings.appSettingTemperatureUnits,
      options = strings.temperatureOptions(),
      selected = preferences.temperatureUnit,
      onDismiss = dismiss,
      onConfirm = { vm.choose(it); dismiss() },
    )

    AppSetting.RegionalFormats -> SingleChoiceSheet(
      title = strings.appSettingRegionalFormats,
      options = strings.regionalFormatOptions(),
      selected = preferences.regionalFormat,
      onDismiss = dismiss,
      onConfirm = { vm.choose(it); dismiss() },
    )

    AppSetting.MapType -> SingleChoiceSheet(
      title = strings.appSettingMapType,
      options = strings.mapTypeOptions(),
      selected = preferences.mapType,
      onDismiss = dismiss,
      onConfirm = { vm.choose(it); dismiss() },
    )

    AppSetting.Language -> SingleChoiceSheet(
      title = strings.appSettingLanguage,
      options = languageOptions(AppStrings),
      // Not `preferences.language` directly: with nothing stored the radio has to point at
      // whichever locale Lyricist resolved the system tag to.
      selected = resolveLanguage(
        stored = preferences.language,
        system = Locale.current.toLanguageTag(),
        available = AppStrings.keys,
      ),
      onDismiss = dismiss,
      onConfirm = { vm.chooseLanguage(it); dismiss() },
    )

    AppSetting.MicrophoneCalibration -> MicCalibrationSheet(
      offset = preferences.microphoneCalibrationOffset,
      onDismiss = dismiss,
      onConfirm = { vm.chooseCalibrationOffset(it); dismiss() },
    )

    // The toggles never call onOpen; the custom data server is TODO(app-settings) — it is a text
    // field, not a picker, and gets its own screen.
    else -> Unit
  }
}