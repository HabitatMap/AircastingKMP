package pl.llp.aircasting.explore

import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.llp.aircasting.domain.GeoLocation
import kotlin.coroutines.resume
import kotlin.getValue

class AndroidPlaceSearch(context: Context) : PlaceSearch {
  private val appContext = context.applicationContext
  private val client: PlacesClient by lazy {
    if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(appContext, placesApiKey())
    Places.createClient(appContext)
  }
  /** Groups the keystrokes and the final fetch into one billed session; a new one starts after each fetch. */
  private var session: AutocompleteSessionToken? = null
  override suspend fun suggestions(query: String): List<PlaceSuggestion> {
    val token = session ?: AutocompleteSessionToken.newInstance().also { session = it }
    val request = FindAutocompletePredictionsRequest.builder().setQuery(query).setSessionToken(token).build()
    val predictions = client.findAutocompletePredictions(request).awaitOrNull()?.autocompletePredictions.orEmpty()
    return predictions.map {
      PlaceSuggestion(it.placeId, it.getPrimaryText(null).toString(), it.getSecondaryText(null).toString())
    }
  }

  override suspend fun location(placeId: String): GeoLocation? {
    val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LOCATION)).setSessionToken(session).build()
    session = null
    return client.fetchPlace(request).awaitOrNull()?.place?.location?.let { GeoLocation(it.latitude, it.longitude) }
  }

  /** Separate from the map key: the manifest's pl.llp.aircasting.PLACES_API_KEY (← PLACES_API_KEY in local.properties). */
  private fun placesApiKey(): String {
    @Suppress("DEPRECATION") // ApplicationInfoFlags overload needs API 33; minSdk here is 28
    val info = appContext.packageManager.getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
    return info.metaData?.getString("pl.llp.aircasting.PLACES_API_KEY").orEmpty()
  }

  /** Null on failure, so callers never see an exception (PlaceSearch contract). */
  private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener {
      log.w(it) { "Places request failed" }
      cont.resume(null)
    }
  }

  private companion object {
    val log = Logger.withTag("PlaceSearch")
  }
}