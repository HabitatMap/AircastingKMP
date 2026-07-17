package com.lunarlogic.aircasting.ui.scan

import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.FailureReason

sealed interface ConnectionUiState {
  data object None : ConnectionUiState
  data object Connecting : ConnectionUiState
  data class Connected(val device: AirBeamDevice) : ConnectionUiState
  data class Failed(val reason: FailureReason) : ConnectionUiState
}