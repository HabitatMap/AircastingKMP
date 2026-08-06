package pl.llp.aircasting.settings.app

import cafe.adriel.lyricist.LanguageTag

data class AppPreferences(
  val crowdMapEnabled: Boolean = true,
  val locationTrackingEnabled: Boolean = true,
  /** `null` = follow the system theme */
  val darkMode: Boolean? = null,
  val pushNotificationsEnabled: Boolean = true,
  val wifiOnlySync: Boolean = false,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.Fahrenheit,
  val regionalFormat: RegionalFormat = RegionalFormat.US,
  val mapType: MapType = MapType.Default,
  /** `null` = follow the system locale */
  val language: LanguageTag? = null,
)
enum class TemperatureUnit { Fahrenheit, Celsius }

enum class DistanceUnit { Miles, Kilometers }

enum class RegionalFormat(
  val datePattern: String,
  val groupingSeparator: Char,
  val decimalSeparator: Char,
  val distanceUnit: DistanceUnit,
) {
  US("MM/DD/YYYY", ',', '.', DistanceUnit.Miles),
  UK("DD/MM/YYYY", ',', '.', DistanceUnit.Miles),
  CentralEuropean("DD.MM.YYYY", '.', ',', DistanceUnit.Kilometers),
  Nordic("DD.MM.YYYY", ' ', ',', DistanceUnit.Kilometers),
  EastAsian("YYYY/MM/DD", ',', '.', DistanceUnit.Kilometers),
}

enum class MapType { Default, Satellite }
