package pl.llp.aircasting.mydata

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.active.chipRank
import pl.llp.aircasting.active.preferred
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.Thresholds
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
  /** `23/11/26, 13:34 - 20:00`, on the session's own wall clock. */
  val timeRange: String,
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
    // A past session always has an end; fall back to the start rather than crash on a bad row.
    timeRange = timeRangeLabel(session.startTime, session.endTime ?: session.startTime, TimeZone.of(session.timeZone)),
    streams = streams,
    selected = streams.preferred(savedSensor) { it.sensorName },
  )
}

private fun StreamSummary.graded(thresholds: Thresholds): SummaryStream {
  val threshold = thresholds.forSensor(sensorName)
  fun grade(value: Double?) = SummaryValue(value, value?.let { threshold?.levelFor(it) })
  return SummaryStream(sensorName, unitSymbol, grade(avg), grade(min), grade(max))
}

/** `DD/MM/YY, HH:mm - DD/MM/YY, HH:mm`, both dates always: a mobile session can cross midnight. */
internal fun timeRangeLabel(start: Instant, end: Instant, zone: TimeZone): String =
  "${dateTimeLabel(start, zone)} - ${dateTimeLabel(end, zone)}"

/** `DD/MM/YY, HH:mm` on [zone]'s wall clock. */
internal fun dateTimeLabel(instant: Instant, zone: TimeZone): String = instant.toLocalDateTime(zone).label()

private fun LocalDateTime.label() = "${date()}, ${time()}"

private fun LocalDateTime.date() = "${day.twoDigits()}/${month.number.twoDigits()}/${(year % 100).twoDigits()}"
private fun LocalDateTime.time() = "${hour.twoDigits()}:${minute.twoDigits()}"
private fun Int.twoDigits() = toString().padStart(2, '0')
