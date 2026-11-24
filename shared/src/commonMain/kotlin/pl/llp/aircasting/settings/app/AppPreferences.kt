package pl.llp.aircasting.settings.app

import cafe.adriel.lyricist.LanguageTag
import pl.llp.aircasting.settings.mic.DefaultCalibrationOffset

data class AppPreferences(
  val isCrowdMapEnabled: Boolean = true,
  val isLocationTrackingEnabled: Boolean = true,
  /** `null` = follow the system theme */
  val isDarkMode: Boolean? = null,
  val isPushNotificationsEnabled: Boolean = true,
  val isWifiOnlySync: Boolean = false,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.Fahrenheit,
  val regionalFormat: RegionalFormat = RegionalFormat.US,
  val mapType: MapType = MapType.Default,
  /** `null` = follow the system locale */
  val language: LanguageTag? = null,
  val microphoneCalibrationOffset: Int = DefaultCalibrationOffset,
  /** `null` = the official AirCasting server */
  val dataServerUrl: String? = null,
)
enum class TemperatureUnit { Fahrenheit, Celsius }

enum class DistanceUnit { Miles, Kilometers }

enum class DateOrder { MonthDayYear, DayMonthYear, YearMonthDay }

/** Formatting presets. Dates are rendered by [pl.llp.aircasting.ui.format.DateFormatter]. */
enum class RegionalFormat(
  val dateOrder: DateOrder,
  val dateSeparator: Char,
  val groupingSeparator: Char,
  val decimalSeparator: Char,
  val distanceUnit: DistanceUnit,
  /** 24-hour `13:34`, or 12-hour `1:34 PM` — CLDR's usual clock for the region (only the US is 12-hour). */
  val use24HourClock: Boolean,
) {
  US(DateOrder.MonthDayYear, '/', ',', '.', DistanceUnit.Miles, use24HourClock = false),
  UK(DateOrder.DayMonthYear, '/', ',', '.', DistanceUnit.Miles, use24HourClock = true),
  CentralEuropean(DateOrder.DayMonthYear, '.', '.', ',', DistanceUnit.Kilometers, use24HourClock = true),
  Nordic(DateOrder.DayMonthYear, '.', ' ', ',', DistanceUnit.Kilometers, use24HourClock = true),
  EastAsian(DateOrder.YearMonthDay, '/', ',', '.', DistanceUnit.Kilometers, use24HourClock = true),
  ;

  /** `MM/DD/YYYY` — the human-readable pattern shown in the Settings picker. */
  val datePattern: String
    get() = when (dateOrder) {
      DateOrder.MonthDayYear -> listOf("MM", "DD", "YYYY")
      DateOrder.DayMonthYear -> listOf("DD", "MM", "YYYY")
      DateOrder.YearMonthDay -> listOf("YYYY", "MM", "DD")
    }.joinToString(dateSeparator.toString())

  /** `h:mm AM` or `HH:mm` — the clock pattern shown in the Settings picker. */
  val timePattern: String
    get() = if (use24HourClock) "HH:mm" else "h:mm AM"
}

enum class MapType { Default, Satellite }
