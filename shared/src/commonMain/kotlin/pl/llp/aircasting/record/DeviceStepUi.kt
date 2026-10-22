package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.scan.ScanUiState

sealed interface DeviceStepUi {
  data object PermissionNeeded : DeviceStepUi
  data object BluetoothOff : DeviceStepUi
  data class Devices(val devices: List<DiscoveredAirBeam>, val scanning: Boolean) : DeviceStepUi {
    val nothingFound: Boolean get() = !scanning && devices.isEmpty()
  }
}

fun deviceStepUi(permissionGranted: Boolean, bluetoothOff: Boolean, scan: ScanUiState): DeviceStepUi = when {
  !permissionGranted -> DeviceStepUi.PermissionNeeded
  bluetoothOff -> DeviceStepUi.BluetoothOff
  else -> when (scan) {
    ScanUiState.Idle -> DeviceStepUi.Devices(emptyList(), scanning = true)
    is ScanUiState.Scanning -> DeviceStepUi.Devices(scan.devices, scanning = true)
    is ScanUiState.Finished -> DeviceStepUi.Devices(scan.devices, scanning = false)
    is ScanUiState.Error -> DeviceStepUi.Devices(emptyList(), scanning = false)
  }
}