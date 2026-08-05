package pl.llp.aircasting.settings.app

import pl.llp.aircasting.i18n.Strings

enum class AppSettingsSection { Community, UnitsRegion, Display, Sensors, Notifications, Sync, Backend }

enum class AppSetting(val section: AppSettingsSection) {
  CrowdMap(AppSettingsSection.Community),
  DisableMapping(AppSettingsSection.Community),
  TemperatureUnits(AppSettingsSection.UnitsRegion),
  RegionalFormats(AppSettingsSection.UnitsRegion),
  Language(AppSettingsSection.UnitsRegion),
  MapType(AppSettingsSection.Display),
  DarkMode(AppSettingsSection.Display),
  MicrophoneCalibration(AppSettingsSection.Sensors),
  PushNotifications(AppSettingsSection.Notifications),
  WifiOnlySync(AppSettingsSection.Sync),
  CustomDataServer(AppSettingsSection.Backend),
}

fun Strings.header(section: AppSettingsSection): String = when (section) {
  AppSettingsSection.Community -> appSettingsCommunityHeader
  AppSettingsSection.UnitsRegion -> appSettingsUnitsRegionHeader
  AppSettingsSection.Display -> appSettingsDisplayHeader
  AppSettingsSection.Sensors -> appSettingsSensorsHeader
  AppSettingsSection.Notifications -> appSettingsNotificationsHeader
  AppSettingsSection.Sync -> appSettingsSyncHeader
  AppSettingsSection.Backend -> appSettingsBackendHeader
}

fun Strings.label(setting: AppSetting): String = when (setting) {
  AppSetting.CrowdMap -> appSettingCrowdMap
  AppSetting.DisableMapping -> appSettingDisableMapping
  AppSetting.TemperatureUnits -> appSettingTemperatureUnits
  AppSetting.RegionalFormats -> appSettingRegionalFormats
  AppSetting.Language -> appSettingLanguage
  AppSetting.MapType -> appSettingMapType
  AppSetting.DarkMode -> appSettingDarkMode
  AppSetting.MicrophoneCalibration -> appSettingMicrophoneCalibration
  AppSetting.PushNotifications -> appSettingPushNotifications
  AppSetting.WifiOnlySync -> appSettingWifiOnlySync
  AppSetting.CustomDataServer -> appSettingCustomDataServer
}

fun Strings.subtitle(setting: AppSetting): String? = when (setting) {
  AppSetting.CrowdMap -> appSettingCrowdMapSubtitle
  AppSetting.DisableMapping -> appSettingDisableMappingSubtitle
  AppSetting.RegionalFormats -> appSettingRegionalFormatsSubtitle
  AppSetting.MicrophoneCalibration -> appSettingMicrophoneCalibrationSubtitle
  AppSetting.PushNotifications -> appSettingPushNotificationsSubtitle
  AppSetting.WifiOnlySync -> appSettingWifiOnlySyncSubtitle
  AppSetting.CustomDataServer -> appSettingCustomDataServerSubtitle
  AppSetting.TemperatureUnits,
  AppSetting.Language,
  AppSetting.MapType,
  AppSetting.DarkMode -> null
}

sealed interface AppSettingRow {
  val setting: AppSetting

  data class Toggle(override val setting: AppSetting, val checked: Boolean) : AppSettingRow
  data class Link(override val setting: AppSetting, val value: String? = null) : AppSettingRow
}