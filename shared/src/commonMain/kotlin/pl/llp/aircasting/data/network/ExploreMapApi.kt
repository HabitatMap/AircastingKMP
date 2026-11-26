package pl.llp.aircasting.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pl.llp.aircasting.domain.ExploreSource
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementType
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * TWEAK HERE: how many levels below the map's real zoom we ask the server to cluster at. The server
 * picks its grid cell size from `zoom`, so a lower zoom = bigger cells = more aggressive clustering.
 * At the real zoom (0), markers in neighbouring cells still overlapped on screen; 1 merges those.
 * Raise it to cluster harder, 0 restores the server's own grid. The server stops clustering at
 * zoom 16, so this also pushes that point 1 level deeper on the map (docs/API_V3.md).
 */
private const val CLUSTER_ZOOM_OFFSET = 1

/** Public, server-clustered map of active fixed sessions + government stations. No auth. */
class ExploreMapApi(private val client: HttpClient) {
  /** Empty [sources] / [measurementTypes] = all, so the param is omitted. */
  suspend fun mapItems(
    viewport: MapViewport,
    sources: Set<ExploreSource>,
    measurementTypes: Set<MeasurementType>,
  ): ExploreMapResponseDto = client.get(ApiEndpoints.V3ExploreMap) {
    parameter("west", viewport.bounds.west)
    parameter("east", viewport.bounds.east)
    parameter("south", viewport.bounds.south)
    parameter("north", viewport.bounds.north)
    parameter("zoom", (viewport.zoom.roundToInt() - CLUSTER_ZOOM_OFFSET).coerceAtLeast(0))
    if (sources.isNotEmpty()) parameter("sources", sources.joinToString(",") { it.apiName })
    if (measurementTypes.isNotEmpty()) {
      parameter("measurement_types", measurementTypes.joinToString(",") { it.apiName })
    }
  }
    .body()

  suspend fun station(uuid: String): StationDetailsDto = client.get(ApiEndpoints.v3ExploreStation(uuid)).body()


  /** Both bounds inclusive, true UTC, at most 12 h apart. Oldest first. */
  suspend fun stationMeasurements(
    uuid: String, sensorName: String, start: Instant, end: Instant,
  ): List<StationMeasurementDto> = client.get(ApiEndpoints.v3ExploreStationMeasurements(uuid)) {
    parameter("sensor_name", sensorName)
    parameter("start_time", start.toEpochMilliseconds())
    parameter("end_time", end.toEpochMilliseconds())
  }.body()
}