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

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import pl.llp.aircasting.domain.resultCount

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ExploreViewModel(
  private val repository: ExploreMapRepository,
  thresholds: ThresholdsRepository,
  location: LocationProvider,
  private val places: PlaceSearch,
) : ViewModel() {
  private val viewport = MutableStateFlow<MapViewport?>(null)
  private val filters = MutableStateFlow(ExploreFilters())
  private val startCentre = MutableStateFlow<GeoLocation?>(null)
  /** The filter sheet's working copy; null = sheet closed. Only [confirmFilters] moves it into [filters]. */
  private val draft = MutableStateFlow<ExploreFilters?>(null)
  private val placeQuery = MutableStateFlow("")
  /** Where the map should jump; the map calls [onFlownTo] once it has. */
  private val flyTo = MutableStateFlow<GeoLocation?>(null)

  init {
    // Once per ViewModel, not per subscription: coming back to the tab must not re-query.
    viewModelScope.launch { startCentre.value = location.current() }
  }

  private val sheet: Flow<FilterSheet?> = combine(draft, viewport, ::Pair)
    .flatMapLatest { (draft, viewport) ->
      when {
        draft == null -> flowOf(null)
        viewport == null -> flowOf(FilterSheet(draft))
        else -> flow {
          emit(FilterSheet(draft))
          emit(FilterSheet(draft, fetch(viewport, draft)?.items?.resultCount()))
        }
      }
    }

  private val loaded: Flow<Loaded> = combine(viewport.filterNotNull(), filters, ::Pair)
    .debounce(CAMERA_SETTLE)
    .mapLatest { (viewport, filters) -> fetch(viewport, filters) }
    .runningFold(Loaded(filters.value, emptyList(), failed = false)) { previous, result ->
      result ?: previous.copy(failed = true)
    }

  /** Blank clears at once; typing waits for a pause, and a newer query drops the lookup in flight. */
  private val suggestions: Flow<List<PlaceSuggestion>> = placeQuery
    .debounce { if (it.isBlank()) 0.milliseconds else TYPING_SETTLE }
    .mapLatest { if (it.isBlank()) emptyList() else places.suggestions(it) }


  private val mapState: Flow<ExploreUiState> =
    combine(loaded, filters, thresholds.observeAll(), startCentre, sheet) { loaded, filters, all, centre, sheet ->
    val byName = all.associateBy { it.sensorName }
      ExploreUiState(
        filters = filters,
        markers = loaded.items.toMarkers(loaded.filters.colourType, byName),
        error = loaded.failed,
        startCentre = centre,
        sheet = sheet,
      )
    }

  val state: StateFlow<ExploreUiState> =
    combine(mapState, suggestions, flyTo) { state, suggestions, flyTo ->
      state.copy(suggestions = suggestions, flyTo = flyTo)
    }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

  fun searchPlaces(query: String) {
    placeQuery.value = query
  }

  fun selectPlace(suggestion: PlaceSuggestion) {
    placeQuery.value = ""
    viewModelScope.launch { places.location(suggestion.id)?.let { flyTo.value = it } }
  }

  fun onFlownTo() {
    flyTo.value = null
  }

  /** Any cluster in the response means we're zoomed out: the whole map turns into plain dots (design frame 4). */
  private fun List<MapItem>.toMarkers(colourType: MeasurementType?, thresholds: Map<String, SensorThreshold>): List<ExploreMarker> {
    val clustered = any { it is MapCluster }
    return map { item ->
      if (clustered || item !is MapStation) ExploreMarker.Dot(item.location)
      else ExploreMarker.Station(item, colourType?.let { item.levelFor(it, thresholds) })
    }
  }

  fun onCameraIdle(viewport: MapViewport) {
    this.viewport.value = viewport
  }

  fun applyFilters(filters: ExploreFilters) {
    this.filters.value = filters
  }

  fun openFilters() {
    draft.value = filters.value
  }

  /** Ignored once the sheet has closed, so a late tap can't reopen it. */
  fun editFilters(edited: ExploreFilters) {
    if (draft.value != null) draft.value = edited
  }

  /** `Done` and `Show N results`. */
  fun confirmFilters() {
    draft.value?.let { filters.value = it }
    draft.value = null
  }

  /** `Cancel`, swipe down, scrim tap. */
  fun dismissFilters() {
    draft.value = null
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
    val TYPING_SETTLE = 300.milliseconds
    val log = Logger.withTag("Explore")
  }
}

/** Any cluster in the response means we're zoomed out: the whole map turns into plain dots (design frame 4). */
private fun List<MapItem>.toMarkers(colourType: MeasurementType?, thresholds: Map<String, SensorThreshold>): List<ExploreMarker> {
  val clustered = any { it is MapCluster }
  return map { item ->
    if (clustered || item !is MapStation) ExploreMarker.Dot(item.location)
    else ExploreMarker.Station(item, colourType?.let { item.levelFor(it, thresholds) })
  }
}