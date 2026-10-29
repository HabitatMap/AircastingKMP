package pl.llp.aircasting.record

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger

@Composable
actual fun rememberWifiNetworks(): WifiNetworks {
  val context = LocalContext.current
  val granted = remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  // Android 12+ only grants fine location when coarse is asked for alongside it.
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
    granted.value = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
  }
  val names = remember { mutableStateOf(emptyList<String>()) }

  if (granted.value) {
    DisposableEffect(context) {
      val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
      val publish = {
        names.value = try {
          @Suppress("DEPRECATION") // SSID: wifiSsid is API 33+, minSdk is 28
          wifi.scanResults.map { ScannedNetwork(it.SSID, it.frequency, it.level) }.twoPointFourGhzNames()
        } catch (e: SecurityException) {
          Logger.withTag("WifiNetworks").w(e) { "Scan results refused" }
          emptyList()
        }
      }
      val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish()
      }
      ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
        ContextCompat.RECEIVER_NOT_EXPORTED,
      )
      publish() // the last scan's results, at once
      @Suppress("DEPRECATION") // still the only way to ask for a fresh scan
      wifi.startScan()
      onDispose { context.unregisterReceiver(receiver) }
    }
  }

  return remember(launcher) {
    object : WifiNetworks {
      override val names get() = names.value
      override val granted get() = granted.value
      override fun request() = launcher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
      )
    }
  }
}