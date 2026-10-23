package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.MeasurementType

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

/** Pill text: the value with its unit when one type is selected (design frames 2–3), else the source badge (frame 1). */
fun ExploreMarker.Station.label(colourType: MeasurementType?): String {
  val value = station.value
  return if (value != null && colourType != null) "$value ${colourType.unitSymbol}"
  else when (station.source) {
    DataSource.AirBeam -> "AIRBEAM"
    DataSource.Government -> "GOV"
  }
}