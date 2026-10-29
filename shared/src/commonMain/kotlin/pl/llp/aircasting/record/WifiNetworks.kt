package pl.llp.aircasting.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

data class ScannedNetwork(val ssid: String, val frequencyMhz: Int, val rssi: Int)

/** AirBeams join 2.4 GHz only. Hidden ones dropped; each name once, strongest first. */
fun List<ScannedNetwork>.twoPointFourGhzNames(): List<String> =
  filter { it.ssid.isNotBlank() && it.frequencyMhz in 2400..2500 }
    .sortedByDescending { it.rssi }
    .map { it.ssid }
    .distinct()

/** Networks to offer in the Wi-Fi name field, and a way to ask for what listing them needs. */
@Stable
interface WifiNetworks {
  /** Empty until known, and stays empty when the platform can't tell. */
  val names: List<String>
  val granted: Boolean
  fun request()
}

/** Android scans nearby networks; iOS can only name the one the phone is on. */
@Composable
expect fun rememberWifiNetworks(): WifiNetworks