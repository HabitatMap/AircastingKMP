package pl.llp.aircasting.explore

import pl.llp.aircasting.domain.ExploreSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.MeasurementType

data class ExploreUiState(
  val filters: ExploreFilters = ExploreFilters(),
  val markers: List<ExploreMarker> = emptyList(),
  /** Last fetch failed; [markers] still hold the last successful result. */
  val error: Boolean = false,
  /** Where the map opens; null = no permission or fix yet, keep the default start. */
  val startCentre: GeoLocation? = null,
  /** The open filter sheet; null = closed. */
  val sheet: FilterSheet? = null,
  /** Places matching the search field; empty = no list. */
  val suggestions: List<PlaceSuggestion> = emptyList(),
  /** One-shot camera jump to a picked place; null once the map has moved. */
  val flyTo: GeoLocation? = null,
  /** Where the camera last came to rest; a re-created map (back from details, tab switch) reopens here. */
  val camera: MapViewport? = null,
)

/** The [draft] being edited and how many results it would show; null [count] = still counting, or the count failed. */
data class FilterSheet(val draft: ExploreFilters, val count: Int? = null)

sealed interface ExploreMarker {
  /** A cluster, or any item of a clustered response */
  data class Dot(val location: GeoLocation) : ExploreMarker
  /** [level] null = draw uncoloured, with the source badge instead of a value. */
  data class Station(val station: MapStation, val level: MeasurementLevel?) : ExploreMarker
}

/** Pill text: the selected type's value with its unit (design frames 2–3), else the source badge (frame 1). */
fun ExploreMarker.Station.label(colourType: MeasurementType?): String {
  val stream = colourType?.let(station::streamFor)
  return if (stream != null) "${stream.value} ${stream.unitSymbol}"
  else when (station.source) {
    ExploreSource.AirBeam -> "AIRBEAM"
    ExploreSource.Government -> "GOV"
  }
}