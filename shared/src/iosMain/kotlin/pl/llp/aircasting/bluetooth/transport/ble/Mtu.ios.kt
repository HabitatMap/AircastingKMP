package pl.llp.aircasting.bluetooth.transport.ble

import com.juul.kable.ServicesDiscoveredPeripheral

internal actual suspend fun ServicesDiscoveredPeripheral.requestMtuIfSupported(mtu: Int) = Unit
