package pl.llp.aircasting.settings.app

import pl.llp.aircasting.i18n.Strings

fun AppPreferences.toRows(strings: Strings, systemDarkMode: Boolean): List<AppSettingRow> =
  AppSetting.entries.map { setting ->
    when (setting) {
      AppSetting.CrowdMap -> AppSettingRow.Toggle(setting, crowdMapEnabled)
      AppSetting.DisableMapping -> AppSettingRow.Toggle(setting, !locationTrackingEnabled)
      AppSetting.DarkMode -> AppSettingRow.Toggle(setting, darkMode ?: systemDarkMode)
      AppSetting.PushNotifications -> AppSettingRow.Toggle(setting, pushNotificationsEnabled)
      AppSetting.WifiOnlySync -> AppSettingRow.Toggle(setting, wifiOnlySync)
      AppSetting.TemperatureUnits -> AppSettingRow.Link(setting, strings.display(temperatureUnit))
      AppSetting.RegionalFormats -> AppSettingRow.Link(setting, strings.display(regionalFormat))
      AppSetting.MapType -> AppSettingRow.Link(setting, strings.display(mapType))
      AppSetting.Language -> AppSettingRow.Link(setting, strings.languageName)
      AppSetting.MicrophoneCalibration ->
        AppSettingRow.Link(setting, microphoneCalibrationOffset.toString())
      AppSetting.CustomDataServer -> AppSettingRow.Link(setting)
    }
  }

fun Strings.display(unit: TemperatureUnit): String = when (unit) {
  TemperatureUnit.Fahrenheit -> appSettingValueFahrenheit
  TemperatureUnit.Celsius -> appSettingValueCelsius
}

fun Strings.display(format: RegionalFormat): String = when (format) {
  RegionalFormat.US -> appSettingValueRegionUs
  RegionalFormat.UK -> appSettingValueRegionUk
  RegionalFormat.CentralEuropean -> appSettingValueRegionCentralEuropean
  RegionalFormat.Nordic -> appSettingValueRegionNordic
  RegionalFormat.EastAsian -> appSettingValueRegionEastAsian
}

fun Strings.display(type: MapType): String = when (type) {
  MapType.Default -> appSettingValueMapDefault
  MapType.Satellite -> appSettingValueMapSatellite
}

fun Strings.display(unit: DistanceUnit): String = when (unit) {
  DistanceUnit.Miles -> unitMiles
  DistanceUnit.Kilometers -> unitKilometers
}