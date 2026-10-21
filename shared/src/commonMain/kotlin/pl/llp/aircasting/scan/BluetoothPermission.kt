package pl.llp.aircasting.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** Whether the app may scan for AirBeams, and a way to ask. Same shape as `MicrophonePermission`. */
@Stable
interface BluetoothPermission {
  val granted: Boolean
  fun request()
}

@Composable
expect fun rememberBluetoothPermission(): BluetoothPermission