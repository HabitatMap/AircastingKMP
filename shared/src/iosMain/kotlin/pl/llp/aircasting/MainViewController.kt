package pl.llp.aircasting

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import pl.llp.aircasting.home.IosLocationPermission

fun MainViewController() = ComposeUIViewController {
  val permission = remember { IosLocationPermission() }
  App(onRequestLocation = { permission.request() })
}