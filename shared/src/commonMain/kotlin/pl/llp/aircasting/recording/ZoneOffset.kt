package pl.llp.aircasting.recording

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt

/**
 * Legacy TimezoneHelper.getTimezoneOffsetInHours: java's rawOffset (standard time, no DST), in whole
 * hours truncated toward zero. kotlinx-datetime has no rawOffset; summer time always adds, so the
 * standard offset is the smaller of a January and a July one, whichever hemisphere.
 */
fun TimeZone.standardOffsetHours(year: Int): Int {
  fun offsetOn(month: Month) = offsetAt(LocalDate(year, month, 1).atStartOfDayIn(this)).totalSeconds
  return minOf(offsetOn(Month.JANUARY), offsetOn(Month.JULY)) / 3600
}
