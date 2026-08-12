package pl.llp.aircasting.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.LanguageTag

/** English copy — the source-of-truth locale. Add `es`/`fr` as sibling values and map them below. */
val EnStrings = Strings(
  authWelcomeTitle = "Welcome to AirCasting!",
  authWelcomeSubtitle = "Sign in or create an account below to monitor your air quality.",
  authSignInSheetSubtitle = "Sign in to continue monitoring your air quality.",
  authSignUpSheetSubtitle = "Monitor and record air quality around you",
  authTabSignIn = "Sign in",
  authTabSignUp = "Create an account",
  authSignInAction = "Sign in",
  authSignUpAction = "Sign up",
  authNoAccountPrompt = "Don't have an account?",
  authHaveAccountPrompt = "Already have an account?",
  authLoginLabel = "Enter email or profile name",
  authEmailLabel = "Enter email",
  authUsernameLabel = "Enter profile name",
  authPasswordLabel = "Enter password",
  authNewPasswordLabel = "Create a password",
  authInvalidCredentials = "Wrong email, profile name or password.",
  authUnexpectedError = "Something went wrong. Please try again.",
  authShowPassword = "Show password",
  authHidePassword = "Hide password",
  authClearField = "Clear",
  authForgotPassword = "Forgot password?",
  forgotPasswordTitle = "Forgot password?",
  forgotPasswordBody =
    "Enter the email associated with your account and we'll send you a reset link.",
  forgotPasswordEmailLabel = "Email",
  forgotPasswordSubmit = "Send reset link",
  forgotPasswordBackToSignIn = "Back to Sign in",
  forgotPasswordSent = "Reset link sent. Check your inbox.",
  forgotPasswordFailed = "Couldn't send the reset link. Please try again.",

  customServerTitle = "Use a custom data server",
  customServerIntroBody = "If you're running your own AirCasting server (self-hosted or " +
    "organizational), point the app to it here. Your existing measurements stay on the " +
    "current server — only new data will sync to the new one.",
  customServerStep1 = "Enter your server's URL and port",
  customServerStep2 = "You'll be logged out to apply the change",
  customServerStep3 = "Log back in. New measurements sync automatically",
  customServerFormBody = "Enter the address of your custom or self-hosted server. " +
    "We'll test the connection before saving anything.",
  customServerUrlLabel = "URL",
  customServerUrlHint = "Enter a valid URL, e.g. https://aircasting.org",
  customServerPortLabel = "Port",
  customServerPortHint = "Port must be a number between 1–65535",
  customServerUseOfficial = "Use official AirCasting server instead",
  customServerNext = "Next",
  customServerTestingTitle = "Testing connection...",
  customServerTestingBody = "Checking if your server responds correctly. This may take a moment.",
  customServerTooLong = "Taking too long? Cancel!",
  customServerVerifiedTitle = "Connection verified",
  customServerVerifiedBody = { server ->
    "$server responded successfully. Your measurements will sync there going forward. " +
      "Existing data will remain on our current server."
  },
  customServerFailedTitle = "Connection failed",
  customServerFailedBody = { server -> "Couldn't reach $server. Check the URL and port, then try again." },
  customServerSave = "Save and log out",
  customServerEdit = "Go back and edit",
  customServerClearField = "Clear",
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
  appSettingValueRegionUs = "US format",
  appSettingValueRegionUk = "UK format",
  appSettingValueRegionCentralEuropean = "Central European",
  appSettingValueRegionNordic = "Nordic",
  appSettingValueRegionEastAsian = "East Asian",
  appSettingFahrenheit = "Fahrenheit",
  appSettingFahrenheitDetail = "°F • eg. 72°F",
  appSettingCelsius = "Celsius",
  appSettingCelsiusDetail = "°C • eg. 22°C",
  unitMiles = "Miles",
  unitKilometers = "Kilometers",
  done = "Done",

  appSettingValueFahrenheit = "°F",
  appSettingValueCelsius = "°C",
  appSettingValueMapDefault = "Default",
  appSettingValueMapSatellite = "Satellite",
  languageName = "English",
  micLiveReading = "LIVE READING",
  micDecibels = "db",
  micCalibrationHint =
    "Typical range: 80–100. Adjust the value below until the live reading matches it.",
  micCalibrationOffset = "CALIBRATION OFFSET",
  micResetOffset = { offset -> "Reset to default offset ($offset)" },
  micDecreaseOffset = "Decrease offset",
  micIncreaseOffset = "Increase offset",
  micPermissionNeeded = "Allow microphone access to see a live reading.",

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
