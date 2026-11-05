package pl.llp.aircasting.domain

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

data class StationDetails(
  val uuid: String,
  val source: DataSource,
  val title: String,
  /** Owner (AirBeam) or agency (gov). */
  val provider: String,
  /** e.g. `AirBeamMini`; null when the station has no device (gov). */
  val deviceModel: String?,
  /** The station's zone: graph times are shown on its wall clock, not the viewer's. */
  val timeZone: TimeZone,
  val streams: List<StationStream>,
)

/**
 * Keyed by [id], not type: PM1 and PM2.5 share `Particulate Matter`. Values rounded by the server.
 * [value]/[measuredAt] null = never reported; [min24h]/[max24h] null = nothing in the last 24 h.
 */
data class StationStream(
  val id: Long,
  val sensorName: String,
  val unitSymbol: String,
  val value: Int?,
  val min24h: Int?,
  val max24h: Int?,
  val measuredAt: Instant?,
)