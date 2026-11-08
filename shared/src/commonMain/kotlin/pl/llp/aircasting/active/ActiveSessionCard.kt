package pl.llp.aircasting.active

import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import kotlin.time.Instant

/** A stream's newest reading as the data layer hands it over; null value/time = nothing recorded yet. */
data class LatestReading(
  val sensorName: String,
  val unitSymbol: String,
  val value: Double? = null,
  val time: Instant? = null,
)

enum class CardState { Live, NoDataYet, ConnectionLost }

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
  /** Newest reading across all streams: drives "1 min ago". */
  val lastSeen: Instant?,
  /** In chip order. */
  val streams: List<CardStream>,
  val selected: CardStream?,
)

fun activeSessionCard(
  session: Session,
  readings: List<LatestReading>,
  thresholds: Thresholds,
  savedSensor: String? = null,
): ActiveSessionCard {
  val streams = readings
    .sortedBy { chipRank(it.sensorName) }
    .map { it.toCardStream(thresholds) }
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
    lastSeen = lastSeen,
    streams = streams,
    // Legacy SessionPresenter.selectStream: saved → PM2.5 → first.
    selected = streams.firstOrNull { it.sensorName == savedSensor }
      ?: streams.firstOrNull { it.sensorName.detailedType == PM2_5 }
      ?: streams.firstOrNull(),
  )
}

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
private fun chipRank(sensorName: String): Int =
  ChipOrder.indexOf(sensorName.detailedType).takeIf { it >= 0 } ?: ChipOrder.size
