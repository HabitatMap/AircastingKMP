package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import kotlin.math.roundToInt

/** Public, server-clustered map of active fixed sessions + government stations. No auth. */
class ExploreMapApi(private val client: HttpClient) {
  /** Empty [sources] / [measurementTypes] = all, so the param is omitted. */
  suspend fun mapItems(
    viewport: MapViewport,
    sources: Set<DataSource>,
    measurementTypes: Set<MeasurementType>,
  ): ExploreMapResponseDto = client.get(ApiEndpoints.V3ExploreMap) {
    parameter("west", viewport.bounds.west)
    parameter("east", viewport.bounds.east)
    parameter("south", viewport.bounds.south)
    parameter("north", viewport.bounds.north)
    parameter("zoom", viewport.zoom.roundToInt())
    if (sources.isNotEmpty()) parameter("sources", sources.joinToString(",") { it.apiName })
    if (measurementTypes.isNotEmpty()) {
      parameter("measurement_types", measurementTypes.joinToString(",") { it.apiName })
    }
  }
    .body()
}