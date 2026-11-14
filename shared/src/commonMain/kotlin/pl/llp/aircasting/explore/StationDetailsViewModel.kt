package pl.llp.aircasting.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
   * [defaultThreshold]: the server default for that sensor, which the Thresholds sheet resets to.
   */
  data class Content(
    val details: StationDetails,
    val selectedStreamId: Long?,
    val threshold: SensorThreshold? = null,
    val defaultThreshold: SensorThreshold? = null,
  ) : StationDetailsState {
    val selected: StationStream? get() = details.streams.find { it.id == selectedStreamId }
    /** Level of any of [selected]'s values — NOW, MIN or MAX all share its threshold. Null value → null. */
    fun levelOf(value: Int?): MeasurementLevel? = value?.let { threshold?.levelFor(it.toDouble()) }
  }
  data object Error : StationDetailsState
}

class StationDetailsViewModel(
  private val details: Flow<StationDetails?>,
  private val thresholds: ThresholdsRepository,
  private val preferredSensor: String? = null,
) : ViewModel() {
  /** The latest emission + the chip; [state] adds the threshold on top. */
  private val loaded = MutableStateFlow<StationDetailsState>(StationDetailsState.Loading)

  /** The current collection of [details]: a retry cancels it before starting a fresh one. */
  private var collecting: Job? = null

  // Thresholds are a live DB flow: an edit elsewhere recolours this screen without refetching.
  val state: StateFlow<StationDetailsState> = combine(loaded, thresholds.observe()) { state, all ->
    if (state !is StationDetailsState.Content) state
    else state.selected?.sensorName.let { sensor ->
      state.copy(threshold = sensor?.let(all::forSensor), defaultThreshold = sensor?.let(all::defaultFor))
    }
  }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StationDetailsState.Loading)

  // Last on purpose: property initialisers and init blocks run top to bottom, so `loaded` and
  // `collecting` must exist first. Collect once here; rotation keeps the ViewModel, so no refetch.
  init { load() }

  /** (Re)collects [details]. Cold one-shot flow → a refetch; live DB flow → a fresh subscription. */
  fun load() {
    collecting?.cancel()
    loaded.value = StationDetailsState.Loading
    collecting = details
      .onEach { new -> loaded.update { it.with(new) } }
      .catch { e ->
        log.w(e) { "Details load failed" }
        loaded.value = StationDetailsState.Error
      }
      .launchIn(viewModelScope)
  }

  /**
   * Folds one emission into the current state. Only the first Content picks the chip
   * (preferred → first); later emissions swap the values and keep the user's choice.
   */
  private fun StationDetailsState.with(new: StationDetails?): StationDetailsState = when {
    new == null -> StationDetailsState.Error
    this is StationDetailsState.Content -> copy(details = new)
    else -> StationDetailsState.Content(
      new,
      selectedStreamId = (new.streams.find { it.sensorName == preferredSensor } ?: new.streams.firstOrNull())?.id,
    )
  }
  /**
   * Slider release or the sheet's Save. [threshold] comes from Thresholds.forSensor, so it is
   * canonically named and one save covers every model spelling. No state update here: the DB flow
   * feeding [state] re-emits it.
   *
   * Values equal to the default drop the override instead of storing a copy, so a sheet "Reset to
   * default" + Save keeps following the server default, like legacy resetToDefaultsClicked.
   */
  fun saveThreshold(threshold: SensorThreshold) {
    val default = (state.value as? StationDetailsState.Content)?.defaultThreshold
    viewModelScope.launch {
      if (threshold == default) thresholds.resetToDefault(threshold.sensorName) else thresholds.save(threshold)
    }
  }

  fun selectStream(id: Long) {
    loaded.update { if (it is StationDetailsState.Content) it.copy(selectedStreamId = id) else it }
  }

  private companion object {
    val log = Logger.withTag("StationDetails")
  }
}