package pl.llp.aircasting.scan

import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.FailureReason

sealed interface ConnectionUiState {
  data object None : ConnectionUiState
  data object Connecting : ConnectionUiState
  data class Connected(val device: AirBeamDevice) : ConnectionUiState
  data class Failed(val reason: FailureReason) : ConnectionUiState
}