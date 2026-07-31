package pl.llp.aircasting

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import pl.llp.aircasting.home.IosLocationPermission
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.AppTab
import pl.llp.aircasting.navigation.TabContent
import pl.llp.aircasting.navigation.label
import pl.llp.aircasting.ui.theme.AircastingTheme
import platform.UIKit.UIViewController

/**
 * One Compose host per tab. SwiftUI's TabView owns tab selection and keeps each controller
 * alive after first use, so each tab keeps its own composition + ViewModelStore.
 */
fun TabViewController(tab: AppTab): UIViewController = ComposeUIViewController {
  val lyricist = rememberStrings(AppStrings)
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      val permission = remember { IosLocationPermission() }
      TabContent(tab, onRequestLocation = { permission.request() })
    }
  }
}

/** Tab titles for the native tab bar — copy stays in Kotlin, Swift only asks for it. */
object TabTitles {
  fun of(tab: AppTab): String = pl.llp.aircasting.i18n.EnStrings.let { it.label(tab) }
  // TODO: locale-aware via a non-Compose Lyricist instance once we ship a 2nd language.
}