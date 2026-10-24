package pl.llp.aircasting.explore

import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.toMapItems
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapItem
import pl.llp.aircasting.domain.MapViewport

interface ExploreMapRepository {
  suspend fun mapItems(viewport: MapViewport, filters: ExploreFilters): List<MapItem>
}

class NetworkExploreMapRepository(private val api: ExploreMapApi) : ExploreMapRepository {
  override suspend fun mapItems(viewport: MapViewport, filters: ExploreFilters): List<MapItem> =
    api.mapItems(
      viewport,
      sources = setOfNotNull(filters.source),
      measurementTypes = filters.measurementTypes,
    )
      .toMapItems()
}