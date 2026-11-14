package pl.llp.aircasting.domain

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.session.SessionType
import kotlin.time.Instant

data class StationDetails(
  val uuid: String,
  val type: SessionType,
  val source: DataSource,
  val title: String,
  /** Owner (AirBeam) or agency (gov). */
  val provider: String,
  /** e.g. `AirBeamMini`; null when the station has no device (gov). */
  val deviceModel: String?,
  /** The station's zone: graph times are shown on its wall clock, not the viewer's. */
  val timeZone: TimeZone,
  val streams: List<StationStream>,
  /** User tags: only our own sessions have them. */
  val tags: List<String> = emptyList(),
  /** Explore only returns outdoor stations. */
  val isIndoor: Boolean = false,
)

/**
 * Keyed by [id], not type: PM1 and PM2.5 share `Particulate Matter`. Values rounded.
 * [value]/[measuredAt] null = never reported. [min]/[max]/[avg] cover the last 24 h for an
 * Explore station and the whole session for our own mobile session; null = nothing in that window.
 * [avg] only exists for our own sessions: Explore doesn't send one.
 */
data class StationStream(
  val id: Long,
  val sensorName: String,
  val unitSymbol: String,
  val value: Int?,
  val min: Int?,
  val max: Int?,
  val measuredAt: Instant?,
  val avg: Int? = null,
)