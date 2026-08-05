package pl.llp.aircasting.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.LanguageTag

/** English copy — the source-of-truth locale. Add `es`/`fr` as sibling values and map them below. */
val EnStrings = Strings(
  settingsVersion = { version -> "AirCasting v$version" },
  settingsTagline = "HabitatMap · Open source air quality",
  settingsTitle = "Settings",
  settingsAccountTitle = "Account settings",
  settingsAccountSubtitle = "Change password, delete account",
  settingsAirBeamsTitle = "AirBeam management",
  settingsAirBeamsSubtitle = "Name, battery, storage, connection",
  settingsAppTitle = "App settings",
  settingsAppSubtitle = "Language, notifications, privacy",
  settingsHelpTitle = "Help",
  settingsHelpSubtitle = "FAQ, resources, app version",

  settingsAccountSectionHeader = "ACCOUNT",
  accountChangeEmail = "Change email",
  accountChangeUsername = "Change username",
  accountResetPassword = "Reset password",
  accountSignOut = "Sign out",
  accountDeleteAccount = "Delete account",
  cancel = "Cancel",
  deleteAccountConfirmTitle = "Delete account?",
  deleteAccountConfirmBody =
    "This permanently deletes your account and every session you've recorded. " +
      "We'll email you a code to confirm.",
  deleteAccountSendCode = "Email me a code",
  deleteAccountCodeTitle = "Confirm deletion",
  // The TTL is in the copy because it is short enough to expire while the user hunts for the
  // email, and a silent 401 twenty minutes later is baffling.
  deleteAccountCodeBody = { email -> "Enter the 4-digit code we sent to $email. It expires in 30 minutes." },
  deleteAccountCodeLabel = "4-digit code",
  deleteAccountCodeInvalid = "That code is wrong or has expired.",
  deleteAccountResend = "Send a new code",
  appSettingsCommunityHeader = "COMMUNITY",
  appSettingsUnitsRegionHeader = "UNITS & REGION",
  appSettingsDisplayHeader = "DISPLAY",
  appSettingsSensorsHeader = "SENSORS",
  appSettingsNotificationsHeader = "NOTIFICATIONS",
  appSettingsSyncHeader = "SYNC",
  appSettingsBackendHeader = "BACKEND",
  appSettingCrowdMap = "Contribute to crowd map",
  appSettingCrowdMapSubtitle = "Share measurements with community",
  appSettingDisableMapping = "Disable mapping",
  appSettingDisableMappingSubtitle = "Stop recording location data",
  appSettingTemperatureUnits = "Temperature units",
  appSettingRegionalFormats = "Regional formats",
  appSettingRegionalFormatsSubtitle = "Date, time, distance and units",
  appSettingLanguage = "Language",
  appSettingMapType = "Map type",
  appSettingDarkMode = "Dark mode",
  appSettingMicrophoneCalibration = "Microphone calibration",
  appSettingMicrophoneCalibrationSubtitle = "Calibrate built-in mic",
  appSettingPushNotifications = "Push notifications",
  appSettingPushNotificationsSubtitle = "Air quality alerts and reminders",
  appSettingWifiOnlySync = "Sync only with WiFi",
  appSettingWifiOnlySyncSubtitle = "Prevents mobile data usage during sync",
  appSettingCustomDataServer = "Custom data server",
  appSettingCustomDataServerSubtitle = "Choose where data is stored",

  appSettingValueFahrenheit = "°F",
  appSettingValueCelsius = "°C",
  appSettingValueRegionUs = "US",
  appSettingValueRegionMetric = "Metric",
  appSettingValueMapDefault = "Default",
  appSettingValueMapSatellite = "Satellite",
  languageName = "English",

  openSettings = "Open settings",
  back = "Back",
  appLogo = "AirCasting",
  errorTitle = "Something went wrong.",
  errorRetry = "Try again",

  airQualityLabel = "AIR QUALITY",
  noReadingsTitle = "No air quality data available",
  noReadingsBody = "We couldn't find current readings for this location.\nCheck again later.",
  noLocationTitle = "No nearby station found",
  noLocationBody = "We couldn't match your location to a reporting station.\nTry enabling precise location.",
  turnOnLocation = "Turn on location services",

  aqGoodLabel = "Good",
  aqGoodDescription = "The air outside is clean. A great time to enjoy activities outside.",
  aqModerateLabel = "Moderate",
  aqModerateDescription = "Air quality is acceptable for most people.",
  aqUnhealthySensitiveLabel = "Unhealthy for sensitive groups",
  aqUnhealthySensitiveDescription = "Sensitive groups should limit outdoor exertion.",
  aqUnhealthyLabel = "Unhealthy",
  aqUnhealthyDescription = "Everyone may begin to feel effects. Limit outdoor time.",
  aqHazardousLabel = "Hazardous",
  aqHazardousDescription = "Health warning. Avoid outdoor activity.",

  pollutantPm25 = "PM 2.5",
  pollutantNo2 = "NO₂",
  pollutantOzone = "Ozone",

  nearbyStationsTitle = "Nearby stations",
  viewMap = "View map",
  govMonitor = "GOV MONITOR",

  distanceAway = { miles -> "$miles mile away" },
  ageJustNow = "just now",
  ageMinutesAgo = { minutes -> "$minutes min ago" },
  ageHoursAgo = { hours -> "$hours hr ago" },
  ageDaysAgo = { days -> "$days d ago" },
  tabHome = "Home",
  tabExplore = "Explore",
  tabRecord = "Record",
  tabFavorites = "Favorites",
  tabMyData = "My data",
)

/** Every available locale, keyed by BCP-47 tag. Lyricist selects one by the system locale at runtime. */
val AppStrings: Map<LanguageTag, Strings> = mapOf(
  "en" to EnStrings,
  // Bare tags only: Lyricist strips the region ("fr-CA" -> "fr") before its second lookup.
  "fr" to FrStrings,
)

/**
 * The active locale's copy, readable anywhere in composition via `LocalStrings.current`.
 * Defaults to [EnStrings] so `@Preview`s and tests resolve without an explicit provider;
 * [pl.llp.aircasting.App] overrides it with the system-selected locale at the root.
 */
val LocalStrings = staticCompositionLocalOf { EnStrings }
