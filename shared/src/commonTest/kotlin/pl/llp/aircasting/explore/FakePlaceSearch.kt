package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.GeoLocation

class FakePlaceSearch(
  private val locations: Map<String, GeoLocation> = emptyMap(),
  private val respond: (String) -> List<PlaceSuggestion> = { emptyList() },
) : PlaceSearch {
  val queries = mutableListOf<String>()

  override suspend fun suggestions(query: String): List<PlaceSuggestion> {
    queries += query
    return respond(query)
  }

  override suspend fun location(placeId: String) = locations[placeId]
}