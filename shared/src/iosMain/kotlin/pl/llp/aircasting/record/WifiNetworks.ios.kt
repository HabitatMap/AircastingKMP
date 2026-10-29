package pl.llp.aircasting.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.NetworkExtension.NEHotspotNetwork

/**
 * iOS can't list nearby networks, only name the one the phone is on — and only with the
 * "Access Wi-Fi Information" entitlement and location permission; otherwise it answers nil.
 * It can't tell the band either, so this may offer a 5 GHz network.
 */
@Composable
actual fun rememberWifiNetworks(): WifiNetworks {
  val names = remember { mutableStateOf(emptyList<String>()) }
  LaunchedEffect(Unit) {
    NEHotspotNetwork.fetchCurrentWithCompletionHandler { network ->
      names.value = listOfNotNull(network?.SSID?.takeIf { it.isNotBlank() })
    }
  }
  return remember {
    object : WifiNetworks {
      override val names get() = names.value
      // Nothing to ask for here: location is asked for elsewhere, the entitlement is build-time.
      override val granted = true
      override fun request() {}
    }
  }
}