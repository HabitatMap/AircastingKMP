package pl.llp.aircasting.mydata

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.active.chipRank
import pl.llp.aircasting.active.preferred
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.recording.PhoneMicDeviceId
import kotlin.time.Instant

/** One aggregate with its colour; level null = no value, or no thresholds for the sensor (grey). */
data class SummaryValue(val value: Double?, val level: MeasurementLevel?)

data class SummaryStream(
  val sensorName: String,
  val unitSymbol: String,
  val avg: SummaryValue,
  val min: SummaryValue,
  val max: SummaryValue,
)

data class PastSessionCard(
  val uuid: String,
  val name: String,
  val tags: List<String>,
  /** Phone-mic sessions are MOBILE rows too, told apart by [PhoneMicDeviceId]: they get their own tag. */
  val isPhoneMic: Boolean,
  /** Fixed sessions get their own tag and open the fixed session details. */
  val isFixed: Boolean,
  /** Formatted by the view with `LocalDateFormatter`, on the session's own wall clock. */
  val startTime: Instant,
  val endTime: Instant,
  val timeZone: TimeZone,
  /** In chip order. */
  val streams: List<SummaryStream>,
  val selected: SummaryStream?,
)

fun pastSessionCard(past: PastSession, thresholds: Thresholds, savedSensor: String? = null): PastSessionCard {
  val session = past.session
  val streams = past.streams
    .sortedBy { chipRank(it.sensorName) }
    .map { it.graded(thresholds) }
  return PastSessionCard(
    uuid = session.uuid,
    name = session.name,
    tags = session.tags,
    isPhoneMic = session.deviceId == PhoneMicDeviceId,
    isFixed = session.type == SessionType.FIXED,
    startTime = session.startTime,
    // A past session always has an end; fall back to the start rather than crash on a bad row.
    endTime = session.endTime ?: session.startTime,
    timeZone = TimeZone.of(session.timeZone),
    streams = streams,
    selected = streams.preferred(savedSensor) { it.sensorName },
  )
}

private fun StreamSummary.graded(thresholds: Thresholds): SummaryStream {
  val threshold = thresholds.forSensor(sensorName)
  fun grade(value: Double?) = SummaryValue(value, value?.let { threshold?.levelFor(it) })
  return SummaryStream(sensorName, unitSymbol, grade(avg), grade(min), grade(max))
}
