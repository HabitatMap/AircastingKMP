package com.lunarlogic.aircasting

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import com.lunarlogic.aircasting.home.IosLocationPermission
import com.lunarlogic.aircasting.i18n.AppStrings
import com.lunarlogic.aircasting.i18n.LocalStrings
import com.lunarlogic.aircasting.navigation.AppTab
import com.lunarlogic.aircasting.navigation.TabContent
import com.lunarlogic.aircasting.navigation.label
import com.lunarlogic.aircasting.ui.theme.AircastingTheme
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
  fun of(tab: AppTab): String = com.lunarlogic.aircasting.i18n.EnStrings.let { it.label(tab) }
  // TODO: locale-aware via a non-Compose Lyricist instance once we ship a 2nd language.
}