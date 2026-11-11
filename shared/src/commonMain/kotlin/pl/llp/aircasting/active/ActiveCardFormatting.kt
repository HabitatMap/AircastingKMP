package pl.llp.aircasting.active

import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.ui.ageLabel
import pl.llp.aircasting.ui.deviceDisplayName
import pl.llp.aircasting.ui.streamChipLabel
import kotlin.math.round
import kotlin.time.Instant

internal fun chipLabel(sensorName: String, strings: Strings): String = when (sensorName.substringAfterLast('-')) {
  "F" -> strings.streamTemperature
  "RH" -> strings.streamHumidity
  "Phone Microphone" -> "dB"
  else -> streamChipLabel(sensorName)
}

/** Rounded the way [pl.llp.aircasting.domain.SensorThreshold.levelFor] rounds, so number and colour agree. */
internal fun valueText(value: Double?): String = value?.let { round(it).toInt().toString() } ?: NO_VALUE

/** `AirBeam Mini • 1 min ago`; the age appears with the first reading. */
internal fun ActiveSessionCard.meta(now: Instant, strings: Strings): List<String> = listOfNotNull(
  deviceModel?.let(::deviceDisplayName),
  lastSeen?.let { ageLabel(it, now, strings) },
)

private const val NO_VALUE = "--"