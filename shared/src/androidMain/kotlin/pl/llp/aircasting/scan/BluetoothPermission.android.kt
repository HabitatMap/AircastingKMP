package pl.llp.aircasting.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal fun bluetoothPermissionsFor(sdkInt: Int): List<String> =
  if (sdkInt >= Build.VERSION_CODES.S) {
    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
  } else {
    listOf(Manifest.permission.ACCESS_FINE_LOCATION)
  }

@Composable
actual fun rememberBluetoothPermission(): BluetoothPermission {
  val context = LocalContext.current
  val permissions = remember { bluetoothPermissionsFor(Build.VERSION.SDK_INT) }
  val state = remember {
    mutableStateOf(
      permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
      },
    )
  }
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { results -> state.value = results.values.all { it } }

  return remember(launcher) {
    object : BluetoothPermission {
      override val granted get() = state.value
      override fun request() = launcher.launch(permissions.toTypedArray())
    }
  }
}