package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.PlaceSearch
import pl.llp.aircasting.explore.PlaceSuggestion
import pl.llp.aircasting.explore.suggestionsFor
import pl.llp.aircasting.recording.PhoneLocation

/** [flyTo]: where the map should jump; the map calls [PinPickerViewModel.onFlownTo] once it has. */
data class PinPickerState(val suggestions: List<PlaceSuggestion> = emptyList(), val flyTo: GeoLocation? = null)

/** Moves the map for the location step. The pin itself lives in the form ([NewSessionViewModel.placePin]). */
class PinPickerViewModel(private val places: PlaceSearch, private val location: PhoneLocation) : ViewModel() {
  private val query = MutableStateFlow("")
  private val flyTo = MutableStateFlow<GeoLocation?>(null)

  val state: StateFlow<PinPickerState> = combine(places.suggestionsFor(query), flyTo, ::PinPickerState)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PinPickerState())

  fun search(text: String) {
    query.value = text
  }

  fun selectPlace(place: PlaceSuggestion) {
    query.value = ""
    viewModelScope.launch { places.location(place.id)?.let { flyTo.value = it } }
  }

  /** Nothing to do until the GPS has a fix. */
  fun locateMe() {
    location.latest()?.let { flyTo.value = it }
  }

  fun onFlownTo() {
    flyTo.value = null
  }
}