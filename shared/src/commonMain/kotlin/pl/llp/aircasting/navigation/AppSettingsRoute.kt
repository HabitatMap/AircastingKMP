package pl.llp.aircasting.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.app.AppSettingsViewModel
import pl.llp.aircasting.settings.app.SettingsAppScreen
import pl.llp.aircasting.settings.app.toRows

@Composable
fun AppSettingsRoute(onBack: () -> Unit) {
  val vm = koinViewModel<AppSettingsViewModel>()
  val preferences by vm.preferences.collectAsStateWithLifecycle()
  val strings = LocalStrings.current
  val systemDarkMode = isSystemInDarkTheme()
  val rows = remember(preferences, strings, systemDarkMode) {
    preferences.toRows(strings, systemDarkMode)
  }
  SettingsAppScreen(
    rows = rows,
    onBack = onBack,
    onToggle = vm::toggle,
    // TODO(app-settings): sub-screens for units, regional formats, language, map type,
    // mic calibration and the custom data server.
    onOpen = { Logger.withTag("AppSettings").d { "$it has no destination yet" } },
  )
}