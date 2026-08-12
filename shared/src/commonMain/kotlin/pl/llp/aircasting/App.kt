package pl.llp.aircasting

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import org.koin.compose.koinInject
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.AppNavHost
import pl.llp.aircasting.settings.app.AppSettingsRepository
import pl.llp.aircasting.ui.SplashGate
import pl.llp.aircasting.ui.theme.AircastingTheme

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  val preferences by koinInject<AppSettingsRepository>().preferences.collectAsStateWithLifecycle()
  val lyricist = rememberStrings(
    AppStrings,
    currentLanguageTag = preferences.language ?: Locale.current.toLanguageTag(),
  )
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme(darkTheme = preferences.darkMode ?: isSystemInDarkTheme()) {
      SplashGate {
        AppNavHost(onRequestLocation)
      }
    }
  }
}
