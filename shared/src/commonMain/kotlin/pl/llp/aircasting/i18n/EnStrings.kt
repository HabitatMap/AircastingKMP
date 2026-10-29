package pl.llp.aircasting.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.LanguageTag

/**
 * The lorem ipsum the Figma onboarding frames still carry. Locale-neutral, so every locale points
 * at it — translating placeholder Latin would only have to be undone.
 */
internal const val OnboardingPlaceholderBody =
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt " +
    "ut labore et dolore magna aliqua."

/** English copy — the source-of-truth locale. Add `es`/`fr` as sibling objects and map them below. */
object EnStrings : Strings {
  override val newSessionConfirmTitle = "Ready to start?"
  override val summaryTypeLabel = "SESSION TYPE"
  override val summaryNameLabel = "SESSION NAME"
  override val summaryTagsLabel = "TAGS"
  override val summaryIntervalLabel = "SAMPLING INTERVAL"
  override val startRecording = "Start recording"
  override val preparingLocating = "Finding your location…"
  override val preparingConfiguring = "Configuring AirBeam…"
  override val latestTagsHeader = "LATEST TAGS"
  override fun addTagLabel(tag: String): String = "Add tag $tag"
  override val samplingIntervalHeader = "SAMPLING INTERVAL"
  override val samplingIntervalHint =
    "Choosing a longer measurement interval will help conserve your device's battery."
  override fun samplingSeconds(count: Int): String = "$count s"
  override fun samplingMinutes(count: Int): String = "$count min"
  override val newSessionDetailsTitle = "Name your session"
  override val newSessionDetailsBody = "Add details so you can find and share it later."
  override val newSessionDeviceTitle = "Choose the device you would like to record with."
  override val newSessionDeviceBody = "Choose the method that fits your setup and session goal."
  override val newSessionDeviceAirBeamHeader = "AIRBEAM"
  override val sessionNameLabel = "Session name"
  override val sessionNamePlaceholder = "eg. \u201CSchool noise pollution\u201D"
  override val sessionTagsLabel = "Tags"
  override val sessionTagsPlaceholder = "Add tags, e.g. \u201Cschool\u201D, \u201Ctraffic\u201D"
  override val methodMobileName = "AirBeam Mobile"
  override val methodFixedName = "AirBeam Fixed"
  override val methodMicName = "Phone Microphone"
  override val methodMobileConfirm = "Review your session details before starting. Keep your AirBeam with you, " +
    "then tap \"Start recording\" below."
  override val methodFixedConfirm = "Review your session details before starting. Hang your AirBeam in a secure " +
    "position, then tap \"Start recording\" below."
  // TODO(design): Figma only draws the AirBeam wording; this mic variant needs sign-off.
  override val methodMicConfirm = "Review your session details before starting. Keep your phone where it can hear " +
    "what you want to measure, then tap \"Start recording\" below."
  override val recordStartTitle = "Start a new recording"
  override val recordStartBody = "Capture air quality and sound levels with AirBeam or your phone."
  override val recordNewSessionTitle = "Start new session"
  override val recordNewSessionBody = "Capture air quality and sound levels with AirBeam or your phone."
  override val recordRecoverTitle = "Recover & sync data"
  override val recordRecoverBody = "Import stored sessions from your AirBeam 3 or AirBeam Mini device."
  override val newSessionMethodTitle = "How will you record?"
  override val newSessionMethodBody = "Choose the method that fits your setup and session goal."
  override val bluetoothPermissionTitle = "Allow nearby devices"
  override val bluetoothPermissionBody = "AirCasting needs this permission to find and connect to your AirBeam."
  override val bluetoothPermissionAllow = "Allow"
  override val bluetoothOffTitle = "Turn on bluetooth"
  override val bluetoothOffBody = "Turn on bluetooth on your phone to connect with the AirBeam device"
  override val noDevicesTitle = "No AirBeam device found"
  override val noDevicesBody = "We couldn't detect an AirBeam nearby. Try these steps to get connected."
  override val noDevicesStepPowerOn = "Make sure your AirBeam is on"
  override val noDevicesStepMoveCloser = "Move closer to your AirBeam"
  override val noDevicesStepBluetoothOn = "Make sure Bluetooth is on"
  override val tryAgain = "Try again"
  override val refresh = "Refresh"
  override val methodMobileTag = "AIRBEAM MOBILE"
  // Figma reads "Air measurment" — typo, fixed here.
  override val methodMobileTitle = "Air measurement on the go"
  override val methodMobileBody =
    "Walk around and capture air pollution in real time with a Bluetooth-connected AirBeam."
  override val methodFixedTag = "AIRBEAM FIXED"
  override val methodFixedTitle = "Air measurement in one place"
  override val methodFixedBody =
    "Monitor a single location continuously, for home, school, or work. Connect via Wi-Fi."
  override val methodMicTag = "PHONE MICROPHONE"
  override val methodMicTitle = "Noise measurement with your phone"
  override val methodMicBody = "Measure noise levels anywhere using your phone's built-in microphone."
  override val next = "Next"
  override val bluetoothPermissionNeeded = "Allow Bluetooth access to find AirBeams nearby."
  // TODO(design): placeholder copy, verbatim from Figma — identical on all three pages until
  // the real copy lands. The per-page fields exist so that swap is a value change, nothing more.
  override val onboardingMeasureTitle = "Placeholder heading"
  override val onboardingMeasureBody = OnboardingPlaceholderBody
  override val onboardingMapTitle = "Placeholder heading"
  override val onboardingMapBody = OnboardingPlaceholderBody
  override val onboardingShareTitle = "Placeholder heading"
  override val onboardingShareBody = OnboardingPlaceholderBody
  override val onboardingNext = "Next step"
  override val onboardingSkip = "Skip"
  override val authWelcomeTitle = "Welcome to AirCasting!"
  override val authWelcomeSubtitle = "Sign in or create an account below to monitor your air quality."
  override val authSignInSheetSubtitle = "Sign in to continue monitoring your air quality."
  override val authSignUpSheetSubtitle = "Monitor and record air quality around you"
  override val authTabSignIn = "Sign in"
  override val authTabSignUp = "Create an account"
  override val authSignInAction = "Sign in"
  override val authSignUpAction = "Sign up"
  override val authNoAccountPrompt = "Don't have an account?"
  override val authHaveAccountPrompt = "Already have an account?"
  override val authLoginLabel = "Enter email or profile name"
  override val authEmailLabel = "Enter email"
  override val authUsernameLabel = "Enter profile name"
  override val authPasswordLabel = "Enter password"
  override val authNewPasswordLabel = "Create a password"
  override val authInvalidCredentials = "Wrong email, profile name or password."
  override val authUnexpectedError = "Something went wrong. Please try again."
  override val authShowPassword = "Show password"
  override val authHidePassword = "Hide password"
  override val authClearField = "Clear"
  override val authForgotPassword = "Forgot password?"
  override val forgotPasswordTitle = "Forgot password?"
  override val forgotPasswordBody =
    "Enter the email associated with your account and we'll send you a reset link."
  override val forgotPasswordEmailLabel = "Email"
  override val forgotPasswordSubmit = "Send reset link"
  override val forgotPasswordBackToSignIn = "Back to Sign in"
  override val forgotPasswordSent = "Reset link sent. Check your inbox."
  override val forgotPasswordFailed = "Couldn't send the reset link. Please try again."

  override val customServerTitle = "Use a custom data server"
  override val customServerIntroBody = "If you're running your own AirCasting server (self-hosted or " +
    "organizational), point the app to it here. Your existing measurements stay on the " +
    "current server — only new data will sync to the new one."
  override val customServerStep1 = "Enter your server's URL and port"
  override val customServerStep2 = "You'll be logged out to apply the change"
  override val customServerStep3 = "Log back in. New measurements sync automatically"
  override val customServerFormBody = "Enter the address of your custom or self-hosted server. " +
    "We'll test the connection before saving anything."
  override val customServerUrlLabel = "URL"
  override val customServerUrlHint = "Enter a valid URL, e.g. https://aircasting.org"
  override val customServerPortLabel = "Port"
  override val customServerPortHint = "Port must be a number between 1–65535"
  override val customServerUseOfficial = "Use official AirCasting server instead"
  override val customServerNext = "Next"
  override val customServerTestingTitle = "Testing connection..."
  override val customServerTestingBody = "Checking if your server responds correctly. This may take a moment."
  override val customServerTooLong = "Taking too long? Cancel!"
  override val customServerVerifiedTitle = "Connection verified"
  override fun customServerVerifiedBody(server: String): String =
    "$server responded successfully. Your measurements will sync there going forward. " +
      "Existing data will remain on our current server."
  override val customServerFailedTitle = "Connection failed"
  override fun customServerFailedBody(server: String): String = "Couldn't reach $server. Check the URL and port, then try again."
  override val customServerSave = "Save and log out"
  override val customServerEdit = "Go back and edit"
  override val customServerClearField = "Clear"
  override fun settingsVersion(version: String): String = "AirCasting v$version"
  override val settingsTagline = "HabitatMap · Open source air quality"
  override val settingsTitle = "Settings"
  override val settingsAccountTitle = "Account settings"
  override val settingsAccountSubtitle = "Change password, delete account"
  override val settingsAirBeamsTitle = "AirBeam management"
  override val settingsAirBeamsSubtitle = "Name, battery, storage, connection"
  override val settingsAppTitle = "App settings"
  override val settingsAppSubtitle = "Language, notifications, privacy"
  override val settingsHelpTitle = "Help"
  override val settingsHelpSubtitle = "FAQ, resources, app version"
  override val helpSectionActions = "AirCasting Actions"
  override val contentLocale = "en-US"
  override val settingsAccountSectionHeader = "ACCOUNT"
  override val accountChangeEmail = "Change email"
  override val accountChangeUsername = "Change username"
  override val accountChangePassword = "Change password"
  override val accountCurrentPasswordLabel = "Current password"
  override val accountNewPasswordLabel = "New password"
  override val accountConfirmPasswordLabel = "Confirm new password"
  override val accountSignOut = "Sign out"
  override val accountDeleteAccount = "Delete account"
  override val accountSave = "Save"
  override val accountUpdateFailed = "Couldn't save. Check your connection and try again."
  override val fieldErrorTaken = "Already in use."
  override val fieldErrorBlank = "Can't be empty."
  override val fieldErrorTooShort: (min: String) -> String = { min -> "Must be at least $min characters." }
  override val fieldErrorTooLong: (max: String) -> String = { max -> "Must be at most $max characters." }
  override val fieldErrorPasswordMismatch = "Passwords don't match."
  override val fieldErrorWrongPassword = "Wrong password."
  override val fieldErrorInvalid = "Invalid value."
  override val accountUpdateRefused = "Couldn't save these changes."
  override val cancel = "Cancel"
  override val deleteAccountConfirmTitle = "Delete account?"
  override val deleteAccountConfirmBody =
    "This permanently deletes your account and every session you've recorded. " +
      "We'll email you a code to confirm."
  override val deleteAccountSendCode = "Email me a code"
  override val deleteAccountCodeTitle = "Confirm deletion"
  // The TTL is in the copy because it is short enough to expire while the user hunts for the
  // email, and a silent 401 twenty minutes later is baffling.
  override fun deleteAccountCodeBody(email: String): String = "Enter the 4-digit code we sent to $email. It expires in 30 minutes."
  override val deleteAccountCodeLabel = "4-digit code"
  override val deleteAccountCodeInvalid = "That code is wrong or has expired."
  override val deleteAccountResend = "Send a new code"
  override val appSettingsCommunityHeader = "COMMUNITY"
  override val appSettingsUnitsRegionHeader = "UNITS & REGION"
  override val appSettingsDisplayHeader = "DISPLAY"
  override val appSettingsSensorsHeader = "SENSORS"
  override val appSettingsNotificationsHeader = "NOTIFICATIONS"
  override val appSettingsSyncHeader = "SYNC"
  override val appSettingsBackendHeader = "BACKEND"
  override val appSettingCrowdMap = "Contribute to crowd map"
  override val appSettingCrowdMapSubtitle = "Share measurements with community"
  override val appSettingDisableMapping = "Disable mapping"
  override val appSettingDisableMappingSubtitle = "Stop recording location data"
  override val appSettingTemperatureUnits = "Temperature units"
  override val appSettingRegionalFormats = "Regional formats"
  override val appSettingRegionalFormatsSubtitle = "Date, time, distance and units"
  override val appSettingLanguage = "Language"
  override val appSettingMapType = "Map type"
  override val appSettingDarkMode = "Dark mode"
  override val appSettingMicrophoneCalibration = "Microphone calibration"
  override val appSettingMicrophoneCalibrationSubtitle = "Calibrate built-in mic"
  override val appSettingPushNotifications = "Push notifications"
  override val appSettingPushNotificationsSubtitle = "Air quality alerts and reminders"
  override val appSettingWifiOnlySync = "Sync only with WiFi"
  override val appSettingWifiOnlySyncSubtitle = "Prevents mobile data usage during sync"
  override val appSettingCustomDataServer = "Custom data server"
  override val appSettingCustomDataServerSubtitle = "Choose where data is stored"
  override val appSettingValueRegionUs = "US format"
  override val appSettingValueRegionUk = "UK format"
  override val appSettingValueRegionCentralEuropean = "Central European"
  override val appSettingValueRegionNordic = "Nordic"
  override val appSettingValueRegionEastAsian = "East Asian"
  override val appSettingFahrenheit = "Fahrenheit"
  override val appSettingFahrenheitDetail = "°F • eg. 72°F"
  override val appSettingCelsius = "Celsius"
  override val appSettingCelsiusDetail = "°C • eg. 22°C"
  override val unitMiles = "Miles"
  override val unitKilometers = "Kilometers"
  override val done = "Done"

  override val appSettingValueFahrenheit = "°F"
  override val appSettingValueCelsius = "°C"
  override val appSettingValueMapDefault = "Default"
  override val appSettingValueMapSatellite = "Satellite"
  override val languageName = "English"
  override val micLiveReading = "LIVE READING"
  override val micDecibels = "db"
  override val micCalibrationHint =
    "Typical range: 80–100. Adjust the value below until the live reading matches it."
  override val micCalibrationOffset = "CALIBRATION OFFSET"
  override fun micResetOffset(offset: Int): String = "Reset to default offset ($offset)"
  override val micDecreaseOffset = "Decrease offset"
  override val micIncreaseOffset = "Increase offset"
  override val micPermissionNeeded = "Allow microphone access to see a live reading."

  override val openSettings = "Open settings"
  override val back = "Back"
  override val appLogo = "AirCasting"
  override val errorTitle = "Something went wrong."
  override val errorRetry = "Try again"

  override val airQualityLabel = "AIR QUALITY"
  override val noReadingsTitle = "No air quality data available"
  override val noReadingsBody = "We couldn't find current readings for this location.\nCheck again later."
  override val noLocationTitle = "No nearby station found"
  override val noLocationBody = "We couldn't match your location to a reporting station.\nTry enabling precise location."
  override val turnOnLocation = "Turn on location services"

  override val aqGoodLabel = "Good"
  override val aqGoodDescription = "The air outside is clean. A great time to enjoy activities outside."
  override val aqModerateLabel = "Moderate"
  override val aqModerateDescription = "Air quality is acceptable for most people."
  override val aqUnhealthySensitiveLabel = "Unhealthy for sensitive groups"
  override val aqUnhealthySensitiveDescription = "Sensitive groups should limit outdoor exertion."
  override val aqUnhealthyLabel = "Unhealthy"
  override val aqUnhealthyDescription = "Everyone may begin to feel effects. Limit outdoor time."
  override val aqHazardousLabel = "Hazardous"
  override val aqHazardousDescription = "Health warning. Avoid outdoor activity."

  override val pollutantPm25 = "PM 2.5"
  override val pollutantNo2 = "NO₂"
  override val pollutantOzone = "Ozone"

  override val nearbyStationsTitle = "Nearby stations"
  override val viewMap = "View map"
  override val govMonitor = "GOV MONITOR"

  override fun distanceAway(miles: Double): String = "$miles mile away"
  override val ageJustNow = "just now"
  override fun ageMinutesAgo(minutes: Long): String = "$minutes min ago"
  override fun ageHoursAgo(hours: Long): String = "$hours hr ago"
  override fun ageDaysAgo(days: Long): String = "$days d ago"
  override val tabHome = "Home"
  override val tabExplore = "Explore"
  override val tabRecord = "Record"
  override val tabFavorites = "Favorites"
  override val tabMyData = "My data"

  override val exploreSearchHint = "Search by name or location"
  override val exploreFilters = "Filters"
  override val exploreFilterSource = "SOURCE"
  override val exploreFilterParameter = "PARAMETER"
  override val exploreShowAll = "Show all"
  override val exploreOnlyAirBeam = "Only AirBeam Fixed"
  override val exploreOnlyGovernment = "Only Government"
  override val exploreSourceAirBeam = "AirBeam Fixed"
  override val exploreSourceGovernment = "Government"
  override val exploreParticulateMatter = "Particulate matter"
  override val exploreNitrogenDioxide = "Nitrogen Dioxide (NO₂)"
  override val exploreOzone = "Ozone"
  override val exploreTemperature = "Temperature"
  override val exploreHumidity = "Humidity"
  override val exploreClearAll = "Clear all"
  override fun exploreShowResults(n: Int?): String =
    when (n) {
      null -> "Show results"
      1 -> "Show 1 result"
      else -> "Show $n results"
    }
  override fun exploreRemoveFilter(name: String): String = "Remove filter: $name"
}

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
val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }
