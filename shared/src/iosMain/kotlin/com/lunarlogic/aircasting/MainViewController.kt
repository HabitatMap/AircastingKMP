package com.lunarlogic.aircasting

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.lunarlogic.aircasting.home.IosLocationPermission

fun MainViewController() = ComposeUIViewController {
  val permission = remember { IosLocationPermission() }
  App(onRequestLocation = { permission.request() })
}