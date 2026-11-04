package pl.llp.aircasting.explore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapItem
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.ThresholdsRepository
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.getOrThrow

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

/** [defaults] keyed by canonical name; [aliases] maps model spellings → canonical, like threshold_sets. */
class FakeThresholdsRepository(
  private val defaults: List<SensorThreshold>,
  private val aliases: Map<String, String> = emptyMap(),
) : ThresholdsRepository {
  private val overrides = MutableStateFlow(emptyList<SensorThreshold>())

  override fun observe(): Flow<Thresholds> = overrides.map { Thresholds(defaults, aliases, it) }
  override suspend fun save(threshold: SensorThreshold) =
    overrides.update { list -> list.filterNot { it.sensorName == threshold.sensorName } + threshold }
  override suspend fun resetToDefault(sensorName: String) =
    overrides.update { list -> list.filterNot { it.sensorName == sensorName } }
}

class FakeStationDetailsRepository(var result: Result<StationDetails>) : StationDetailsRepository {
  val uuids = mutableListOf<String>()

  override suspend fun station(uuid: String): StationDetails {
    uuids += uuid
    return result.getOrThrow()
  }
}

class FakeStationMeasurementsRepository(var result: Result<List<ChartPoint>>) : StationMeasurementsRepository {
  val streamIds = mutableListOf<Long>()

  override suspend fun measurements(streamId: Long): List<ChartPoint> {
    streamIds += streamId
    return result.getOrThrow()
  }
}