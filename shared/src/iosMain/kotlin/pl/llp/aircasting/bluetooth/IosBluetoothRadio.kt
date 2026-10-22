package pl.llp.aircasting.bluetooth

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.darwin.NSObject

class IosBluetoothRadio : BluetoothRadio {
  private var alertManager: CBCentralManager? = null

  override val isOff: Flow<Boolean> = callbackFlow {
    val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
      override fun centralManagerDidUpdateState(central: CBCentralManager) {
        trySend(central.state == CBManagerStatePoweredOff)
      }
    }
    // Observing stays silent; only requestEnable() is allowed to raise the system alert.
    val manager = CBCentralManager(
      delegate = delegate,
      queue = null,
      options = mapOf<Any?, Any>(CBCentralManagerOptionShowPowerAlertKey to false),
    )
    // CBCentralManager holds its delegate weakly — referencing it here keeps it alive while collected.
    awaitClose { if (manager.delegate === delegate) manager.delegate = null }
  }.distinctUntilChanged()

  override fun requestEnable() {
    // A manager created with ShowPowerAlert while Bluetooth is off makes iOS show "Turn On Bluetooth".
    alertManager = CBCentralManager(
      delegate = null,
      queue = null,
      options = mapOf<Any?, Any>(CBCentralManagerOptionShowPowerAlertKey to true),
    )
  }
}