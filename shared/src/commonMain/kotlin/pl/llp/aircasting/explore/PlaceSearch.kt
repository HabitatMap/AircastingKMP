package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.GeoLocation

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
