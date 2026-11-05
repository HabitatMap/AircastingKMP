package pl.llp.aircasting.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.ThresholdsRepository

sealed interface StationDetailsState {
  data object Loading : StationDetailsState
  /**
   * [selectedStreamId] null only if the station came with no streams.
   * [threshold]: the user's local one for [selected]'s sensor; null = none stored → uncoloured.
   */
  data class Content(
    val details: StationDetails,
    val selectedStreamId: Long?,
    val threshold: SensorThreshold? = null,
  ) : StationDetailsState {
    val selected: StationStream? get() = details.streams.find { it.id == selectedStreamId }
    /** Level of any of [selected]'s values — NOW, MIN or MAX all share its threshold. Null value → null. */
    fun levelOf(value: Int?): MeasurementLevel? = value?.let { threshold?.levelFor(it.toDouble()) }
  }
  data object Error : StationDetailsState
}

class StationDetailsViewModel(
  private val uuid: String,
  private val repository: StationDetailsRepository,
  private val thresholds: ThresholdsRepository,
) : ViewModel() {
  /** What the network gave us + the chip; [state] adds the threshold on top. */
  private val loaded = MutableStateFlow<StationDetailsState>(StationDetailsState.Loading)

  // Thresholds are a live DB flow: an edit elsewhere recolours this screen without refetching.
  val state: StateFlow<StationDetailsState> = combine(loaded, thresholds.observe()) { state, all ->
    if (state !is StationDetailsState.Content) state
    else state.copy(threshold = state.selected?.let { all.forSensor(it.sensorName) })
  }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StationDetailsState.Loading)

  // After `loaded` on purpose: property initialisers and init blocks run top to bottom.
  // The uuid is fixed for this ViewModel's life, so load once here; rotation doesn't refetch.
  init { load() }

  fun load() {
    loaded.value = StationDetailsState.Loading
    viewModelScope.launch {
      loaded.value = try {
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

  /**
   * Slider release (later: the sheet's Save). [threshold] comes from Thresholds.forSensor, so it is
   * canonically named and one save covers every model spelling. No state update here: the DB flow
   * feeding [state] re-emits it.
   */
  fun saveThreshold(threshold: SensorThreshold) {
    viewModelScope.launch { thresholds.save(threshold) }
  }

  fun selectStream(id: Long) {
    loaded.update { if (it is StationDetailsState.Content) it.copy(selectedStreamId = id) else it }
  }

  private companion object {
    val log = Logger.withTag("StationDetails")
  }
}