package pl.llp.aircasting.bluetooth.transport.ble

import com.juul.kable.ServicesDiscoveredPeripheral

/**
 * Asks for a bigger MTU where the app has to (Android starts at 23 bytes, too small for an
 * AirBeam frame). iOS negotiates on its own. A refusal is logged, not fatal — as in legacy.
 */
internal expect suspend fun ServicesDiscoveredPeripheral.requestMtuIfSupported(mtu: Int)
