package pl.llp.aircasting.settings.app

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
)
enum class TemperatureUnit { Fahrenheit, Celsius }

enum class RegionalFormat { US, Metric }

enum class MapType { Default, Satellite }
