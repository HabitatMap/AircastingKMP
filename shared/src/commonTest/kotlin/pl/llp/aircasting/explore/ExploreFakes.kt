package pl.llp.aircasting.explore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapItem
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.ThresholdsRepository

class FakeExploreMapRepository(
  private val respond: suspend (MapViewport, ExploreFilters) -> List<MapItem> = { _, _ -> emptyList() },
) : ExploreMapRepository {
  val calls = mutableListOf<Pair<MapViewport, ExploreFilters>>()
  /** Viewports whose call returned — a cancelled call never lands here. */
  val completed = mutableListOf<MapViewport>()

  override suspend fun mapItems(viewport: MapViewport, filters: ExploreFilters): List<MapItem> {
    calls += viewport to filters
    return respond(viewport, filters).also { completed += viewport }
  }
}

class FakeThresholdsRepository(initial: List<SensorThreshold>) : ThresholdsRepository {
  private val all = MutableStateFlow(initial)

  override fun observeAll(): Flow<List<SensorThreshold>> = all
  override fun observeBySensorName(sensorName: String): Flow<SensorThreshold?> =
    all.map { list -> list.find { it.sensorName == sensorName } }
  override suspend fun findBySensorName(sensorName: String): SensorThreshold? =
    all.value.find { it.sensorName == sensorName }
  override suspend fun save(threshold: SensorThreshold) {
    all.value = all.value.filterNot { it.sensorName == threshold.sensorName } + threshold
  }
  override suspend fun delete(sensorName: String) {
    all.value = all.value.filterNot { it.sensorName == sensorName }
  }
}