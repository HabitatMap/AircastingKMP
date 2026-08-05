package pl.llp.aircasting.settings.app

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.enums.enumEntries

interface AppSettingsRepository {
  val preferences: StateFlow<AppPreferences>
  fun update(transform: (AppPreferences) -> AppPreferences)
}

class StoredAppSettingsRepository(private val settings: Settings) : AppSettingsRepository {

  private val _preferences = MutableStateFlow(settings.readPreferences())
  override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

  override fun update(transform: (AppPreferences) -> AppPreferences) {
    val next = transform(_preferences.value)
    settings.writePreferences(next)
    _preferences.value = next
  }
}

private const val KeyCrowdMap = "crowd_map_enabled"
private const val KeyLocationTracking = "location_tracking_enabled"
private const val KeyDarkMode = "dark_mode"
private const val KeyPushNotifications = "push_notifications_enabled"
private const val KeyWifiOnlySync = "wifi_only_sync"
private const val KeyTemperatureUnit = "temperature_unit"
private const val KeyRegionalFormat = "regional_format"
private const val KeyMapType = "map_type"
private const val KeyLanguage = "language"

private val Defaults = AppPreferences()

private fun Settings.readPreferences() = AppPreferences(
  language = getStringOrNull(KeyLanguage),
  crowdMapEnabled = getBoolean(KeyCrowdMap, Defaults.crowdMapEnabled),
  locationTrackingEnabled = getBoolean(KeyLocationTracking, Defaults.locationTrackingEnabled),
  darkMode = getBooleanOrNull(KeyDarkMode),
  pushNotificationsEnabled = getBoolean(KeyPushNotifications, Defaults.pushNotificationsEnabled),
  wifiOnlySync = getBoolean(KeyWifiOnlySync, Defaults.wifiOnlySync),
  temperatureUnit = getEnum(KeyTemperatureUnit, Defaults.temperatureUnit),
  regionalFormat = getEnum(KeyRegionalFormat, Defaults.regionalFormat),
  mapType = getEnum(KeyMapType, Defaults.mapType),
)

private fun Settings.writePreferences(prefs: AppPreferences) {
  prefs.language?.let { putString(KeyLanguage, it) } ?: remove(KeyLanguage)
  putBoolean(KeyCrowdMap, prefs.crowdMapEnabled)
  putBoolean(KeyLocationTracking, prefs.locationTrackingEnabled)
  // Absent, not false: "not chosen yet" has to stay distinguishable from "chosen: light".
  prefs.darkMode?.let { putBoolean(KeyDarkMode, it) } ?: remove(KeyDarkMode)
  putBoolean(KeyPushNotifications, prefs.pushNotificationsEnabled)
  putBoolean(KeyWifiOnlySync, prefs.wifiOnlySync)
  putString(KeyTemperatureUnit, prefs.temperatureUnit.name)
  putString(KeyRegionalFormat, prefs.regionalFormat.name)
  putString(KeyMapType, prefs.mapType.name)
}

private inline fun <reified T : Enum<T>> Settings.getEnum(key: String, default: T): T =
  getStringOrNull(key)?.let { stored -> enumEntries<T>().firstOrNull { it.name == stored } }
    ?: default