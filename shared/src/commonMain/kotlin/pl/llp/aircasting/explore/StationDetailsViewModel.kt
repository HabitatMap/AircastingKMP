package pl.llp.aircasting.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream

sealed interface StationDetailsState {
  data object Loading : StationDetailsState
  /** [selectedStreamId] null only if the station came with no streams. */
  data class Content(
    val details: StationDetails,
    val selectedStreamId: Long?,
    val threshold: SensorThreshold? = null,
  ) : StationDetailsState {
    val selected: StationStream? get() = details.streams.find { it.id == selectedStreamId }
    /** Level of any of [selected]'s values — NOW, MIN or MAX all share its threshold. */
    fun levelOf(value: Int): MeasurementLevel? = threshold?.levelFor(value.toDouble())
  }
  data object Error : StationDetailsState
}

class StationDetailsViewModel(
  private val uuid: String,
  private val repository: StationDetailsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<StationDetailsState>(StationDetailsState.Loading)
  val state: StateFlow<StationDetailsState> = _state.asStateFlow()

  // The uuid is fixed for this ViewModel's life, so load once here; rotation doesn't refetch.
  init { load() }

  fun load() {
    _state.value = StationDetailsState.Loading
    viewModelScope.launch {
      _state.value = try {
        val details = repository.station(uuid)
        StationDetailsState.Content(details, details.streams.firstOrNull()?.id)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        log.w(e) { "Station $uuid load failed" }
        StationDetailsState.Error
      }
    }
  }
  fun selectStream(id: Long) {
    _state.update { if (it is StationDetailsState.Content) it.copy(selectedStreamId = id) else it }
  }

  private companion object {
    val log = Logger.withTag("StationDetails")
  }
}