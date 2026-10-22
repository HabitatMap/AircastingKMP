package pl.llp.aircasting.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.LinkState
import kotlin.time.Duration.Companion.seconds

/**
 * One scan's length. Legacy relied on classic discovery ending by itself (~12 s); the BLE scan
 * never ends, so it's time-boxed to tell "still scanning" from "nothing found".
 */
internal val SCAN_WINDOW = 15.seconds

class ScanViewModel(
  private val connector: AirBeamConnector,
  private val link: AirBeamLink,
) : ViewModel() {
  private val attempts = MutableStateFlow(0)

  @OptIn(ExperimentalCoroutinesApi::class)
  val state: StateFlow<ScanUiState> =
    attempts
      .flatMapLatest { scanOnce() }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState.Idle,
      )

  fun rescan() = attempts.update { it + 1 }

  private fun scanOnce(): Flow<ScanUiState> = channelFlow {
    send(ScanUiState.Scanning(emptyList()))
    var found = emptyList<DiscoveredAirBeam>()
    withTimeoutOrNull(SCAN_WINDOW) {
      connector.scan()
        .collect { found = it; send(ScanUiState.Scanning(it)) }
    }
    send(ScanUiState.Finished(found))
  }.catch { emit(ScanUiState.Error(FailureReason.RadioOrPermissionMissing)) }

  val connection: StateFlow<ConnectionUiState> =
    link.state.map { linkState ->
      when (linkState) {
        LinkState.Idle -> ConnectionUiState.None
        is LinkState.Connecting -> ConnectionUiState.Connecting
        is LinkState.Connected -> ConnectionUiState.Connected(linkState.target.device)
        is LinkState.Reconnecting -> ConnectionUiState.Connecting
        is LinkState.Failed -> ConnectionUiState.Failed(linkState.reason)
      }
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConnectionUiState.None,
      )

  /** The link owns the attempt; leaving the screen doesn't cancel it. */
  fun onConnectClicked(target: DiscoveredAirBeam) {
    viewModelScope.launch { link.connect(target) }
  }
}
