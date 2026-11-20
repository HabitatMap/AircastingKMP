package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.scan.ScanUiState

sealed interface DeviceStepUi {
  data class Devices(val devices: List<DiscoveredAirBeam>, val scanning: Boolean) : DeviceStepUi {
    val nothingFound: Boolean get() = !scanning && devices.isEmpty()
  }
}

/** Bluetooth permission and power are settled before the picker; only the scan is left. */
fun deviceStepUi(scan: ScanUiState): DeviceStepUi.Devices = when (scan) {
  ScanUiState.Idle -> DeviceStepUi.Devices(emptyList(), scanning = true)
  is ScanUiState.Scanning -> DeviceStepUi.Devices(scan.devices, scanning = true)
  is ScanUiState.Finished -> DeviceStepUi.Devices(scan.devices, scanning = false)
  is ScanUiState.Error -> DeviceStepUi.Devices(emptyList(), scanning = false)
}
