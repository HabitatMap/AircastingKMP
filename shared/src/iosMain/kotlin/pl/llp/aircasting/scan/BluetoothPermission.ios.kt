package pl.llp.aircasting.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationDenied
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun rememberBluetoothPermission(): BluetoothPermission = remember {
  object : BluetoothPermission {
    // "Not determined" counts as granted: there is no request API, the first scan raises the prompt.
    override val granted: Boolean
      get() = CBManager.authorization.let {
        it != CBManagerAuthorizationDenied && it != CBManagerAuthorizationRestricted
      }

    // Once denied, iOS never re-prompts — Settings is the only way back.
    override fun request() {
      val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
      UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
  }
}