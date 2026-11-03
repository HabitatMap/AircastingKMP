package pl.llp.aircasting.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.llp.aircasting.ui.chart.ChartPoint

sealed interface StationGraphState {
  data object Loading : StationGraphState
  /** Sorted by x, 24 h ending at the newest point. */
  data class Ready(val points: List<ChartPoint>) : StationGraphState
  data object Error : StationGraphState
}

/**
 * One stream's graph. One instance per stream (keyed by stream id at the call site), so switching
 * chips back and forth doesn't refetch, and a failed graph never takes the header down with it.
 */
class StationGraphViewModel(
  private val streamId: Long,
  private val repository: StationMeasurementsRepository,
  // Sorting 86 400 points takes long enough to drop frames on the main thread.
  private val compute: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
  private val _state = MutableStateFlow<StationGraphState>(StationGraphState.Loading)
  val state: StateFlow<StationGraphState> = _state.asStateFlow()

  init { load() }

  fun load() {
    _state.value = StationGraphState.Loading
    viewModelScope.launch {
      _state.value = try {
        StationGraphState.Ready(withContext(compute) { last24h(repository.measurements(streamId)) })
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        log.w(e) { "Stream $streamId graph load failed" }
        StationGraphState.Error
      }
    }
  }
  private companion object {
    val log = Logger.withTag("StationGraph")
  }
}