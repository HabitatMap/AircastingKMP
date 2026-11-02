package pl.llp.aircasting.domain

import kotlin.time.Instant

/** An Explore station's details: `/explore/stations/{uuid}`. */
data class StationDetails(
  val source: DataSource,
  val title: String,
  /** Owner (AirBeam) or agency (gov). */
  val provider: String,
  /** e.g. `AirBeamMini`; null when the station has no device (gov). */
  val deviceModel: String?,
  val streams: List<StationStream>,
)

/** Keyed by [id], not type: PM1 and PM2.5 share `Particulate Matter`. Values rounded by the server. */
data class StationStream(
  val id: Long,
  val sensorName: String,
  val unitSymbol: String,
  val value: Int,
  val min24h: Int,
  val max24h: Int,
  val measuredAt: Instant,
)