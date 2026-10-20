package pl.llp.aircasting.bluetooth.transport.ble

import co.touchlab.kermit.Logger
import com.juul.kable.ServicesDiscoveredPeripheral
import kotlinx.coroutines.CancellationException

internal actual suspend fun ServicesDiscoveredPeripheral.requestMtuIfSupported(mtu: Int) {
  try {
    val negotiated = requestMtu(mtu)
    Logger.withTag("BleMtu").d { "MTU negotiated: $negotiated" }
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (e: Exception) {
    Logger.withTag("BleMtu").w(e) { "MTU request for $mtu refused" }
  }
}
