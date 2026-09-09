package pl.llp.aircasting.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamSessionController
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.SessionAction
import pl.llp.aircasting.bluetooth.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ScanViewModel(
  private val connector: AirBeamConnector,
  private val sessionController: AirBeamSessionController,
) : ViewModel() {
  val state: StateFlow<ScanUiState> =
    connector.scan()
      .map<List<DiscoveredAirBeam>, ScanUiState> { ScanUiState.Scanning(it) }
      .catch { emit(ScanUiState.Error(FailureReason.RadioOrPermissionMissing)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState.Idle,
      )

  val connection: StateFlow<ConnectionUiState> =
    sessionController.state.map { sessionState ->
      when (sessionState) {
        SessionState.Idle -> ConnectionUiState.None
        is SessionState.Connecting -> ConnectionUiState.Connecting
        is SessionState.Connected -> ConnectionUiState.Connected(sessionState.device)
        is SessionState.Configuring -> ConnectionUiState.Connected(sessionState.device)
        is SessionState.Recording -> ConnectionUiState.Connected(sessionState.device)
        is SessionState.Reconnecting -> ConnectionUiState.Connecting
        is SessionState.Failed -> ConnectionUiState.Failed(sessionState.reason)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = ConnectionUiState.None,
    )

  fun onConnectClicked(target: DiscoveredAirBeam) {
    sessionController.dispatch(SessionAction.Connect(target))
  }
}