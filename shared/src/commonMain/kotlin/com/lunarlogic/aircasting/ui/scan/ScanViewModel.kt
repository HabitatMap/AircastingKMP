package com.lunarlogic.aircasting.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(private val connector: AirBeamConnector) : ViewModel() {
  val state: StateFlow<ScanUiState> =
    connector.scan()
      .map<List<DiscoveredAirBeam>, ScanUiState> { ScanUiState.Scanning(it) }
      .catch { emit(ScanUiState.Error(FailureReason.RadioOrPermissionMissing)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState.Idle,
      )

  private val _connection = MutableStateFlow<ConnectionUiState>(ConnectionUiState.None)
  val connection: StateFlow<ConnectionUiState> = _connection.asStateFlow()

  fun onConnectClicked(target: DiscoveredAirBeam) {
    viewModelScope.launch {
      _connection.value = ConnectionUiState.Connecting
      val conn = connector.connect(target)
      val outcome = conn.status.first {
        it is ConnectionStatus.Ready || it is ConnectionStatus.Failed
      }
      _connection.value = when (outcome) {
        is ConnectionStatus.Ready -> ConnectionUiState.Connected(outcome.device)
        is ConnectionStatus.Failed -> ConnectionUiState.Failed(outcome.reason)
        else -> error("first { } guarantees Ready or Failed")
      }
    }
  }
}