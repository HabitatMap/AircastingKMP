package pl.llp.aircasting.active

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A stream's newest reading as the data layer hands it over; null value/time = nothing recorded yet. */
data class LatestReading(
  val sensorName: String,
  val unitSymbol: String,
  val value: Double? = null,
  val time: Instant? = null,
)

enum class CardState {
  Live,
  NoDataYet,
  /** Mobile: the AirBeam's Bluetooth link dropped. */
  ConnectionLost,
  /** Fixed: its last measurement is older than [FixedOfflineAfter]. */
  Offline,
}

/**
 * TWEAK HERE: how old a fixed AirBeam's last measurement may get before its card says "Offline".
 * It uploads about once a minute, so 30 min ≈ thirty missed uploads (*decided 2026-11-24*). The backend's
 * own `is_active` (Explore) uses 24 h, too slow for the "lost its Wi-Fi" card. Lower = flags a dead
 * station sooner but flickers on spotty Wi-Fi; higher = calmer but slower to notice.
 */
internal val FixedOfflineAfter = 30.minutes

data class CardStream(
  val sensorName: String,
  val unitSymbol: String,
  val value: Double?,
  /** Null when there's no value yet or no thresholds for the sensor: drawn grey. */
  val level: MeasurementLevel?,
)

data class ActiveSessionCard(
  val uuid: String,
  val name: String,
  val tags: List<String>,
  val deviceModel: String?,
  val state: CardState,
  /**
   * With [lastSeen] as the end: the card's time row, formatted by the view on this zone's clock.
   * Null only for a fixed session the server has no measurement for yet: no time row.
   */
  val startTime: Instant?,
  val timeZone: TimeZone,
  /** Newest reading across all streams: drives "1 min ago" and ends the time row. */
  val lastSeen: Instant?,
  /** In chip order. */
  val streams: List<CardStream>,
  val selected: CardStream?,
  /** Fixed only: leads the meta row as Indoor / Outdoor. Null = mobile, no placement shown. */
  val isIndoor: Boolean? = null,
)

fun activeSessionCard(
  session: Session,
  readings: List<LatestReading>,
  thresholds: Thresholds,
  savedSensor: String? = null,
): ActiveSessionCard {
  val streams = readings.toCardStreams(thresholds)
  val lastSeen = readings.mapNotNull { it.time }.maxOrNull()
  return ActiveSessionCard(
    uuid = session.uuid,
    name = session.name,
    tags = session.tags,
    deviceModel = session.deviceModel,
    state = when {
      session.status == SessionStatus.DISCONNECTED -> CardState.ConnectionLost
      lastSeen == null -> CardState.NoDataYet
      else -> CardState.Live
    },
    startTime = session.startTime,
    timeZone = TimeZone.of(session.timeZone),
    lastSeen = lastSeen,
    streams = streams,
    selected = streams.preferred(savedSensor) { it.sensorName },
  )
}

/**
 * A fixed card. Offline is judged on [ActiveFixedSession.lastReportedAt] (when the AirBeam last
 * reached the server), falling back to the newest reading; neither → nothing reported yet.
 */
fun activeFixedSessionCard(
  session: ActiveFixedSession,
  thresholds: Thresholds,
  savedSensor: String? = null,
  now: Instant,
): ActiveSessionCard {
  val streams = session.readings.toCardStreams(thresholds)
  val lastSeen = session.readings.mapNotNull { it.time }.maxOrNull()
  val lastHeard = session.lastReportedAt ?: lastSeen
  return ActiveSessionCard(
    uuid = session.uuid,
    name = session.name,
    tags = session.tags,
    deviceModel = session.deviceModel,
    state = when {
      lastHeard == null -> CardState.NoDataYet
      now - lastHeard > FixedOfflineAfter -> CardState.Offline
      else -> CardState.Live
    },
    startTime = session.startTime,
    timeZone = TimeZone.of(session.timeZone),
    lastSeen = lastSeen,
    streams = streams,
    selected = streams.preferred(savedSensor) { it.sensorName },
    isIndoor = session.isIndoor,
  )
}

private fun List<LatestReading>.toCardStreams(thresholds: Thresholds) =
  sortedBy { chipRank(it.sensorName) }.map { it.toCardStream(thresholds) }

/** Legacy SessionPresenter.selectStream: saved → PM2.5 → first. */
internal fun <T> List<T>.preferred(savedSensor: String?, sensorName: (T) -> String): T? =
  firstOrNull { sensorName(it) == savedSensor }
    ?: firstOrNull { sensorName(it).detailedType == PM2_5 }
    ?: firstOrNull()

private fun LatestReading.toCardStream(thresholds: Thresholds) = CardStream(
  sensorName = sensorName,
  unitSymbol = unitSymbol,
  value = value,
  level = value?.let { thresholds.forSensor(sensorName)?.levelFor(it) },
)

/** `AirBeam3-PM2.5` → `PM2.5`: what legacy calls the stream's detailed type. */
private val String.detailedType get() = substringAfterLast('-')

private const val PM2_5 = "PM2.5"

/** Figma 461:2780 chip order. Legacy puts F first; the redesign moves it after the PMs. */
private val ChipOrder = listOf("PM1", PM2_5, "PM10", "F", "RH")

/** Unknown sensors go last; sortedBy is stable, so they keep the order they came in. */
internal fun chipRank(sensorName: String): Int =
  ChipOrder.indexOf(sensorName.detailedType).takeIf { it >= 0 } ?: ChipOrder.size
