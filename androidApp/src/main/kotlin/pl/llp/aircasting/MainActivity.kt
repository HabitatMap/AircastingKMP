package pl.llp.aircasting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private val LOCATION_PERMISSIONS = arrayOf(
  Manifest.permission.ACCESS_FINE_LOCATION,
  Manifest.permission.ACCESS_COARSE_LOCATION,
)

// TEMP: asked with location at startup until the connect step asks in context.
private val BLUETOOTH_PERMISSIONS =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
  } else {
    emptyArray()
  }

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
      ) { /* result picked up by App's ON_RESUME reload — nothing to do here */ }

      App(
        onRequestLocation = {
          // Either location grant is enough; each Bluetooth one is needed.
          val location = if (LOCATION_PERMISSIONS.any(::granted)) emptyList() else LOCATION_PERMISSIONS.toList()
          val missing = location + BLUETOOTH_PERMISSIONS.filterNot(::granted)
          if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
        },
      )
    }
  }

  private fun granted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}
