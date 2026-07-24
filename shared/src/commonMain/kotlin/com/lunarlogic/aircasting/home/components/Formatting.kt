package com.lunarlogic.aircasting.home.components

import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.i18n.Strings
import kotlin.math.round
import kotlin.time.Instant

internal fun Pollutant.label(strings: Strings): String = when (this) {
  Pollutant.PM25 -> strings.pollutantPm25
  Pollutant.NO2 -> strings.pollutantNo2
  Pollutant.OZONE -> strings.pollutantOzone
}

internal fun distanceLabel(meters: Double, strings: Strings): String {
  val miles = round(meters / 1609.344 * 10) / 10.0
  return strings.distanceAway(miles)
}

/** Coarse "x ago" label for [instant] relative to [now]. Presentation-facing; the caller supplies
 *  the clock reading so this stays pure and testable. */
internal fun ageLabel(instant: Instant, now: Instant, strings: Strings): String {
  val secs = (now - instant).inWholeSeconds.coerceAtLeast(0)
  return when {
    secs < 60 -> strings.ageJustNow
    secs < 3600 -> strings.ageMinutesAgo(secs / 60)
    secs < 86_400 -> strings.ageHoursAgo(secs / 3600)
    else -> strings.ageDaysAgo(secs / 86_400)
  }
}
