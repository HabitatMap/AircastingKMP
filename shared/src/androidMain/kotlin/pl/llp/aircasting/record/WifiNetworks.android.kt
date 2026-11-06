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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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
    val wifi = remember { context.applicationContext.getSystemService(WifiManager::class.java) }
    DisposableEffect(wifi) {
      val publish = {
        try {
          @Suppress("DEPRECATION") // SSID: wifiSsid is API 33+, minSdk is 28
          val latest = wifi.scanResults.map { ScannedNetwork(it.SSID, it.frequency, it.level) }.twoPointFourGhzNames()
          names.value = names.value.withNewlySeen(latest)
        } catch (e: SecurityException) {
          Logger.withTag("WifiNetworks").w(e) { "Scan results refused" }
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
      onDispose { context.unregisterReceiver(receiver) }
    }
    // Android allows a foreground app 4 scans per 2 minutes: one every 30 s keeps the list growing.
    LaunchedEffect(wifi) {
      while (true) {
        @Suppress("DEPRECATION") // still the only way to ask for a fresh scan
        wifi.startScan()
        delay(RescanEvery)
      }
    }
  }

  return remember(launcher) {
    object : WifiNetworks {
      override val names get() = names.value
      override val granted get() = granted.value
      override val listOnly = true
      override fun request() = launcher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
      )
    }
  }
}
private val RescanEvery = 30.seconds