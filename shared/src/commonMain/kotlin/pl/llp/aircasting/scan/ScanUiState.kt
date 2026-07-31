package pl.llp.aircasting.scan

import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason

sealed interface ScanUiState {
  data object Idle : ScanUiState
  data class Scanning(val devices: List<DiscoveredAirBeam>) : ScanUiState
  data class Error(val reason: FailureReason) : ScanUiState
}