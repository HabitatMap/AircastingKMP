package pl.llp.aircasting.active

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * One of our own fixed sessions as the Active tab needs it. Nothing about fixed sessions is stored
 * locally (the AirBeam uploads by itself, see `FixedSessionStarter`), so this comes from the backend.
 */
data class ActiveFixedSession(
  val uuid: String,
  val name: String,
  val tags: List<String>,
  val deviceModel: String?,
  val isIndoor: Boolean,
  /** IANA zone at the sensor. */
  val timeZone: String,
  /** Null until the server has a first measurement. */
  val startTime: Instant?,
  /** When the AirBeam last reached the server; null = never. Drives the "Offline" state. */
  val lastReportedAt: Instant?,
  val readings: List<LatestReading>,
)

interface ActiveFixedSessionsRepository {
  /** Fixed sessions not finished yet, newest first, re-fetched periodically. Empty when signed out. */
  fun observeFixed(): Flow<List<ActiveFixedSession>>

  /** Fetch now instead of waiting for the next poll (Reconnect). */
  fun refresh()

  /** Finishes the session on the server; its card goes once the answer has finished_at. Failures are logged only. */
  suspend fun finish(uuid: String)
}
