package pl.llp.aircasting

import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.settingsGraph
import pl.llp.aircasting.ui.theme.AircastingTheme
import platform.UIKit.UIViewController

fun SettingsViewController(onClose: () -> Unit): UIViewController = ComposeUIViewController {
  val lyricist = rememberStrings(AppStrings)
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      val nav = rememberNavController()
      NavHost(navController = nav, startDestination = SettingsRoute.Root) {
        settingsGraph(nav, onExit = onClose)
      }
    }
  }
}
