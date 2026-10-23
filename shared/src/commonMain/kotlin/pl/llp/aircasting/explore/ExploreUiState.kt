package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MeasurementLevel

data class ExploreUiState(
  val filters: ExploreFilters = ExploreFilters(),
  val markers: List<ExploreMarker> = emptyList(),
  /** Last fetch failed; [markers] still hold the last successful result. */
  val error: Boolean = false,
)

sealed interface ExploreMarker {
  data class Cluster(val cluster: MapCluster) : ExploreMarker
  /** [level] null = draw uncoloured, with the source badge instead of a value. */
  data class Station(val station: MapStation, val level: MeasurementLevel?) : ExploreMarker
}