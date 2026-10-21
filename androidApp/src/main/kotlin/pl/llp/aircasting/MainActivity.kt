package pl.llp.aircasting

import android.Manifest
import android.content.pm.PackageManager
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
        onRequestLocation = { if (!hasLocationPermission()) launcher.launch(LOCATION_PERMISSIONS) },
      )
    }
  }

  private fun hasLocationPermission() = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
  }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}
