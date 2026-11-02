package pl.llp.aircasting.ui

import pl.llp.aircasting.i18n.Strings
import kotlin.time.Instant

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
