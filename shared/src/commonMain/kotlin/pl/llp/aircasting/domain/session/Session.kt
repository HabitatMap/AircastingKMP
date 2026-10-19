package pl.llp.aircasting.domain.session

import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Duration
import kotlin.time.Instant

enum class SessionType { MOBILE, FIXED }

enum class SessionStatus { NEW, RECORDING, DISCONNECTED, FINISHED }
data class Session(
  val uuid: String,
  val type: SessionType,
  val status: SessionStatus,
  val name: String,
  val tags: List<String>,
  val startTime: Instant,
  /** IANA zone id. v3 create requires it, and it's mandatory for indoor sessions. */
  val timeZone: String,
  // TODO: We won't have averaging
  /** Rule #4: averaging is skipped when this is >= the averaging window. */
  val measurementInterval: Duration,
  val endTime: Instant? = null,
  val deviceId: String? = null,
  val deviceModel: String? = null,
  /** Per-session fix for fixed sessions; mobile carries location per measurement instead. */
  val location: GeoLocation? = null,
  val isIndoor: Boolean = false,
  val contribute: Boolean = true,
  /** v3 fixed only: the Bearer token the AirBeam itself uses to upload. Not the user token. */
  val sessionToken: String? = null,
  /** Mobile only: the server has our `end_time` as `finished_at`. */
  val finishSynced: Boolean = false,
)