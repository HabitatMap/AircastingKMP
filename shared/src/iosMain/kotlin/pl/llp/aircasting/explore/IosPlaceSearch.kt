package pl.llp.aircasting.explore

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.llp.aircasting.domain.GeoLocation
import platform.Foundation.NSBundle
import swiftPMImport.Aircasting.shared.GMSAutocompleteRequest
import swiftPMImport.Aircasting.shared.GMSAutocompleteSessionToken
import swiftPMImport.Aircasting.shared.GMSAutocompleteSuggestion
import swiftPMImport.Aircasting.shared.GMSFetchPlaceRequest
import swiftPMImport.Aircasting.shared.GMSPlacePropertyCoordinate
import swiftPMImport.Aircasting.shared.GMSPlacesClient
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosPlaceSearch : PlaceSearch {
  /** Same key as the map (Info.plist GMSApiKey); Places has its own provideAPIKey. */
  private val client: GMSPlacesClient by lazy {
    GMSPlacesClient.provideAPIKey(NSBundle.mainBundle.objectForInfoDictionaryKey("GMSApiKey") as? String ?: "")
    GMSPlacesClient.sharedClient()
  }
  /** Groups the keystrokes and the final fetch into one billed session; a new one starts after each fetch. */
  private var session: GMSAutocompleteSessionToken? = null

  override suspend fun suggestions(query: String): List<PlaceSuggestion> {
    val token = session ?: GMSAutocompleteSessionToken().also { session = it }
    val request = GMSAutocompleteRequest(query = query).apply { sessionToken = token }
    return suspendCancellableCoroutine { cont ->
      // GooglePlaces calls back on the main queue.
      client.fetchAutocompleteSuggestionsFromRequest(request) { results, error ->
        if (error != null) log.w { "Autocomplete failed: ${error.localizedDescription}" }
        val places = results.orEmpty().mapNotNull { (it as? GMSAutocompleteSuggestion)?.placeSuggestion }
        cont.resume(places.map { PlaceSuggestion(it.placeID, it.attributedPrimaryText.string, it.attributedSecondaryText?.string.orEmpty()) })
      }
    }
  }
  override suspend fun location(placeId: String): GeoLocation? {
    val request = GMSFetchPlaceRequest(placeID = placeId, placeProperties = listOf(GMSPlacePropertyCoordinate), sessionToken = session)
    session = null
    return suspendCancellableCoroutine { cont ->
      client.fetchPlaceWithRequest(request) { place, error ->
        if (error != null) log.w { "Place fetch failed: ${error.localizedDescription}" }
        cont.resume(place?.coordinate?.useContents { GeoLocation(latitude, longitude) })
      }
    }
  }

  private companion object {
    val log = Logger.withTag("PlaceSearch")
  }
}