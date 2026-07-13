package com.lunarlogic.aircasting.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ScanViewModel(connector: AirBeamConnector) : ViewModel() {
  val state: StateFlow<ScanUiState> =
    connector.scan()
      .map<List<DiscoveredAirBeam>, ScanUiState> { ScanUiState.Scanning(it) }
      .catch { emit(ScanUiState.Error(FailureReason.RadioOrPermissionMissing)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState.Idle,
      )
}