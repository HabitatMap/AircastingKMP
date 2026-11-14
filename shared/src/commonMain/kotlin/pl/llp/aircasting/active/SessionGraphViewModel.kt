package pl.llp.aircasting.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.explore.StationGraphState

/**
 * One stream's graph for our own session: the whole session, live. One instance per stream
 * (keyed by stream id at the call site), like StationGraphViewModel.
 */
class SessionGraphViewModel(
  private val uuid: String,
  private val sensorName: String,
  private val timeZone: TimeZone,
  private val repository: SessionDetailsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<StationGraphState>(StationGraphState.Loading)
  val state: StateFlow<StationGraphState> = _state.asStateFlow()

  /** The current subscription: a retry cancels it before starting a fresh one. */
  private var collecting: Job? = null

  // Last: `_state` and `collecting` must be initialised before load() runs.
  init { load() }

  fun load() {
    collecting?.cancel()
    _state.value = StationGraphState.Loading
    collecting = repository.observePoints(uuid, sensorName, timeZone)
      .onEach { _state.value = StationGraphState.Ready(it) }
      .catch { e ->
        log.w(e) { "Session $uuid $sensorName graph failed" }
        _state.value = StationGraphState.Error
      }
      .launchIn(viewModelScope)
  }
  private companion object {
    val log = Logger.withTag("SessionGraph")
  }
}