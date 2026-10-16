package pl.llp.aircasting.domain.session

import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Instant

data class Measurement(
  val streamId: Long,
  val time: Instant,
  val value: Double,
  /** Mobile only — a fixed session's position lives on the Session. */
  val location: GeoLocation? = null,
)