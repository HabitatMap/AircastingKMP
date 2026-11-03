package pl.llp.aircasting.explore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Duration.Companion.milliseconds

/**
 * Google Places autocomplete, behind the platform SDKs (Places SDK for Android / GooglePlaces for iOS).
 * Never throws — a failed lookup is an empty list / null, like [pl.llp.aircasting.home.LocationProvider].
 */
interface PlaceSearch {
  suspend fun suggestions(query: String): List<PlaceSuggestion>
  /** Ends the billing session that [suggestions] opened. */
  suspend fun location(placeId: String): GeoLocation?
}

/** [primary] = place name ("Kraków"), [secondary] = where it is ("Poland"); may be empty. */
data class PlaceSuggestion(val id: String, val primary: String, val secondary: String)

/** Blank clears at once; typing waits for a pause, and a newer query drops the lookup in flight. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun PlaceSearch.suggestionsFor(queries: Flow<String>): Flow<List<PlaceSuggestion>> = queries
  .debounce { if (it.isBlank()) 0.milliseconds else TypingSettle }
  .mapLatest { if (it.isBlank()) emptyList() else suggestions(it) }

private val TypingSettle = 300.milliseconds