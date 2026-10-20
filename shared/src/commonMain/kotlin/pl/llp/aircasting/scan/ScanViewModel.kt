package pl.llp.aircasting.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.LinkState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(
  private val connector: AirBeamConnector,
  private val link: AirBeamLink,
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
    link.state.map { linkState ->
      when (linkState) {
        LinkState.Idle -> ConnectionUiState.None
        is LinkState.Connecting -> ConnectionUiState.Connecting
        is LinkState.Connected -> ConnectionUiState.Connected(linkState.target.device)
        is LinkState.Reconnecting -> ConnectionUiState.Connecting
        is LinkState.Failed -> ConnectionUiState.Failed(linkState.reason)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = ConnectionUiState.None,
    )

  /** The link owns the attempt; leaving the screen doesn't cancel it. */
  fun onConnectClicked(target: DiscoveredAirBeam) {
    viewModelScope.launch { link.connect(target) }
  }
}
