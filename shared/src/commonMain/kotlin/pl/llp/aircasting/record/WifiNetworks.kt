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

/** Networks listed so far plus any [latest] scan found anew, appended so listed rows keep their place. */
fun List<String>.withNewlySeen(latest: List<String>): List<String> = (this + latest).distinct()

/** Networks to offer in the Wi-Fi name field, and a way to ask for what listing them needs. */
@Stable
interface WifiNetworks {
  /** Empty until known, and stays empty when the platform can't tell. */
  val names: List<String>
  val granted: Boolean
  /** True where [names] is a real scan (Android): pick from it only, as legacy did. False where it can't be (iOS): typing stays. */
  val listOnly: Boolean
  fun request()
}

/** Android scans nearby networks; iOS can only name the one the phone is on. */
@Composable
expect fun rememberWifiNetworks(): WifiNetworks