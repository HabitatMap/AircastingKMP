package pl.llp.aircasting.scan

import android.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals

class BluetoothPermissionsTest {

  @Test
  fun `Android 12 and newer ask for the dedicated Bluetooth permissions`() {
    assertEquals(
      listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
      bluetoothPermissionsFor(sdkInt = 31),
    )
  }

  @Test
  fun `Android 11 and older ask for fine location, which gates BLE scans there`() {
    assertEquals(
      listOf(Manifest.permission.ACCESS_FINE_LOCATION),
      bluetoothPermissionsFor(sdkInt = 30),
    )
  }
}