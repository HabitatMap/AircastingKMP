package pl.llp.aircasting.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothRadio {
  /** `true` while the phone's Bluetooth is switched off. */
  val isOff: Flow<Boolean>

  /** Ask the OS to switch Bluetooth on: a system dialog on Android, the system power alert on iOS. */
  fun requestEnable()
}