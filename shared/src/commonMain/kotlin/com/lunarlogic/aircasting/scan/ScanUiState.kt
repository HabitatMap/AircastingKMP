package com.lunarlogic.aircasting.scan

import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason

sealed interface ScanUiState {
  data object Idle : ScanUiState
  data class Scanning(val devices: List<DiscoveredAirBeam>) : ScanUiState
  data class Error(val reason: FailureReason) : ScanUiState
}