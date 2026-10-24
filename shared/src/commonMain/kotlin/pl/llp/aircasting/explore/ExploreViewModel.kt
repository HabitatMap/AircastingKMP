package pl.llp.aircasting.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapItem
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.ThresholdsRepository
import pl.llp.aircasting.domain.levelFor
import pl.llp.aircasting.home.LocationProvider
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ExploreViewModel(
  private val repository: ExploreMapRepository,
  thresholds: ThresholdsRepository,
  location: LocationProvider,
) : ViewModel() {
  private val viewport = MutableStateFlow<MapViewport?>(null)
  private val filters = MutableStateFlow(ExploreFilters())
  private val startCentre = MutableStateFlow<GeoLocation?>(null)

  init {
    // Once per ViewModel, not per subscription: coming back to the tab must not re-query.
    viewModelScope.launch { startCentre.value = location.current() }
  }

  private val loaded: Flow<Loaded> = combine(viewport.filterNotNull(), filters, ::Pair)
    .debounce(CAMERA_SETTLE)
    .mapLatest { (viewport, filters) -> fetch(viewport, filters) }
    .runningFold(Loaded(filters.value, emptyList(), failed = false)) { previous, result ->
      result ?: previous.copy(failed = true)
    }

  val state: StateFlow<ExploreUiState> =
    combine(loaded, filters, thresholds.observeAll(), startCentre) { loaded, filters, all, centre ->
      val byName = all.associateBy { it.sensorName }
      ExploreUiState(
        filters = filters,
        markers = loaded.items.map { it.toMarker(loaded.filters.colourType, byName) },
        error = loaded.failed,
        startCentre = centre,
      )
    }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

  fun onCameraIdle(viewport: MapViewport) {
    this.viewport.value = viewport
  }

  fun applyFilters(filters: ExploreFilters) {
    this.filters.value = filters
  }

  /** Null on failure. Cancellation is rethrown so mapLatest can drop a stale request. */
  private suspend fun fetch(viewport: MapViewport, filters: ExploreFilters): Loaded? = try {
    Loaded(filters, repository.mapItems(viewport, filters), failed = false)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    log.w(e) { "Map fetch failed for $viewport" }
    null
  }

  /** Items plus the filters they were fetched with — their values belong to those filters. */
  private data class Loaded(val filters: ExploreFilters, val items: List<MapItem>, val failed: Boolean)

  private companion object {
    val CAMERA_SETTLE = 300.milliseconds
    val log = Logger.withTag("Explore")
  }
}

private fun MapItem.toMarker(colourType: MeasurementType?, thresholds: Map<String, SensorThreshold>): ExploreMarker =
  when (this) {
    is MapCluster -> ExploreMarker.Cluster(this)
    is MapStation -> ExploreMarker.Station(this, colourType?.let { levelFor(it, thresholds) })
  }