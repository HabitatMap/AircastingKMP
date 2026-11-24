package pl.llp.aircasting.ui.format

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.settings.app.DateOrder
import pl.llp.aircasting.settings.app.RegionalFormat
import kotlin.time.Instant

/**
 * The one place user-facing dates are formatted, in the order and separator of the user's
 * Settings → Regional formats choice, with its clock. Short year, as the designs show it:
 * US `11/23/26, 1:34 PM`, UK `23/11/26, 13:34`, Central European `23.11.26, 13:34`,
 * East Asian `26/11/23, 13:34`.
 *
 * Models and ViewModels carry [Instant]s + the zone, never preformatted dates; composables format
 * through [LocalDateFormatter], which `App` provides from the stored preference — so a change in
 * Settings re-renders every date without any ViewModel knowing about it.
 */
class DateFormatter(regionalFormat: RegionalFormat) {

  private val format = LocalDateTime.Format {
    date(shortDate(regionalFormat.dateOrder, regionalFormat.dateSeparator))
    chars(", ")
    time(clock(regionalFormat.use24HourClock, withSeconds = false))
  }

  private val timeWithSecondsFormat = clock(regionalFormat.use24HourClock, withSeconds = true)

  /** e.g. `23/11/26, 13:34` or `11/23/26, 1:34 PM`, on [zone]'s wall clock. */
  fun dateTime(instant: Instant, zone: TimeZone): String = format.format(instant.toLocalDateTime(zone))

  /** e.g. `13:34:07` or `1:34:07 PM` — graph and map tooltips, where 1 s readings need the seconds. */
  fun timeWithSeconds(instant: Instant, zone: TimeZone): String =
    timeWithSecondsFormat.format(instant.toLocalDateTime(zone).time)

  /** Both dates always: a session can cross midnight. No [end] yet (no measurement) → the start alone. */
  fun range(start: Instant, end: Instant?, zone: TimeZone): String =
    end?.let { "${dateTime(start, zone)} - ${dateTime(it, zone)}" } ?: dateTime(start, zone)
}

private fun shortDate(order: DateOrder, separator: Char) = LocalDate.Format {
  val parts = when (order) {
    DateOrder.MonthDayYear -> listOf(Part.Month, Part.Day, Part.Year)
    DateOrder.DayMonthYear -> listOf(Part.Day, Part.Month, Part.Year)
    DateOrder.YearMonthDay -> listOf(Part.Year, Part.Month, Part.Day)
  }
  parts.forEachIndexed { i, part ->
    if (i > 0) char(separator)
    when (part) {
      Part.Day -> day()
      Part.Month -> monthNumber()
      Part.Year -> yearTwoDigits(baseYear = 2000)
    }
  }
}

private enum class Part { Day, Month, Year }

/** `HH:mm[:ss]`, or US-style `h:mm[:ss] AM` (no leading zero on the hour). */
private fun clock(use24HourClock: Boolean, withSeconds: Boolean) = LocalTime.Format {
  if (use24HourClock) hour() else amPmHour(padding = Padding.NONE)
  char(':')
  minute()
  if (withSeconds) {
    char(':')
    second()
  }
  if (!use24HourClock) {
    char(' ')
    amPmMarker(am = "AM", pm = "PM")
  }
}

/** Provided by `App` from the stored [RegionalFormat]; the default matches the preference default. */
val LocalDateFormatter = staticCompositionLocalOf { DateFormatter(RegionalFormat.US) }
