package pl.llp.aircasting.i18n

/**
 * All user-facing copy for the app, as plain Kotlin — one instance per locale.
 *
 * Because the copy is Kotlin (not XML), formatting is trivial:
 *  - line breaks: put `\n` in the string, or use a `"""triple-quoted"""` multi-line literal;
 *  - parameterised copy: declare it as a `fun` (e.g. `strings.ageMinutesAgo(3)`), with full
 *    type-safety on the arguments.
 *
 * Add a new language by creating a sibling `object EsStrings : Strings` and registering it in
 * [AppStrings]. The members here are the single source of truth every locale must fill in.
 *
 * An interface rather than a data class on purpose: a constructor taking every string as a
 * parameter hits the JVM's 255-slot method-signature limit (the class then fails to load at
 * runtime). Abstract members have no such cap, and the compiler still rejects a locale `object`
 * that leaves any of them out.
 */
interface Strings {
  // Record tab — start screen
  val recordStartTitle: String
  val recordStartBody: String
  val recordNewSessionTitle: String
  val recordNewSessionBody: String
  val recordRecoverTitle: String
  val recordRecoverBody: String
  // New session wizard — "How will you record?"
  val newSessionMethodTitle: String
  val newSessionMethodBody: String
  val newSessionDeviceTitle: String
  val newSessionDeviceBody: String
  val newSessionDeviceAirBeamHeader: String
  val methodMobileTag: String
  val methodMobileTitle: String
  val methodMobileBody: String
  val methodFixedTag: String
  val methodFixedTitle: String
  val methodFixedBody: String
  val methodMicTag: String
  val methodMicTitle: String
  val methodMicBody: String
  val methodMobileName: String
  val methodFixedName: String
  val methodMicName: String
  val methodMobileConfirm: String
  val methodFixedConfirm: String
  val methodMicConfirm: String
  val next: String
  val bluetoothPermissionNeeded: String
  val bluetoothPermissionAllow: String
  // New session wizard — "Name your session"
  val newSessionDetailsTitle: String
  val newSessionDetailsBody: String
  val sessionNameLabel: String
  val sessionNamePlaceholder: String
  val sessionTagsLabel: String
  val sessionTagsPlaceholder: String
  val latestTagsHeader: String
  fun addTagLabel(tag: String): String
  val samplingIntervalHeader: String
  val samplingIntervalHint: String
  fun samplingSeconds(count: Int): String
  fun samplingMinutes(count: Int): String
  // New session wizard — "Ready to start?"
  val newSessionConfirmTitle: String
  val summaryTypeLabel: String
  val summaryNameLabel: String
  val summaryTagsLabel: String
  val summaryIntervalLabel: String
  val startRecording: String
  val preparingLocating: String
  val preparingConfiguring: String
  // New session wizard — device step states
  val bluetoothPermissionTitle: String
  val bluetoothPermissionBody: String
  val bluetoothOffTitle: String
  val bluetoothOffBody: String
  val noDevicesTitle: String
  val noDevicesBody: String
  val noDevicesStepPowerOn: String
  val noDevicesStepMoveCloser: String
  val noDevicesStepBluetoothOn: String
  val tryAgain: String
  val refresh: String
  // Onboarding — TODO(design): Figma still has lorem ipsum, this copy needs sign-off
  val onboardingMeasureTitle: String
  val onboardingMeasureBody: String
  val onboardingMapTitle: String
  val onboardingMapBody: String
  val onboardingShareTitle: String
  val onboardingShareBody: String
  val onboardingNext: String
  val onboardingSkip: String
  // Auth — headers
  val authWelcomeTitle: String
  val authWelcomeSubtitle: String
  val authSignInSheetSubtitle: String
  val authSignUpSheetSubtitle: String
  // Auth — tabs & actions
  val authTabSignIn: String
  val authTabSignUp: String
  val authSignInAction: String
  val authSignUpAction: String
  val authNoAccountPrompt: String
  val authHaveAccountPrompt: String
  // Auth — field labels (they float above the input, so they read as instructions)
  val authLoginLabel: String
  val authEmailLabel: String
  val authUsernameLabel: String
  val authPasswordLabel: String
  val authNewPasswordLabel: String
  // Auth — errors the backend can't phrase for us
  val authInvalidCredentials: String
  val authUnexpectedError: String
  // Auth — accessibility labels for icon-only controls
  val authShowPassword: String
  val authHidePassword: String
  val authClearField: String
  // Forgot password
  val authForgotPassword: String
  val forgotPasswordTitle: String
  val forgotPasswordBody: String
  val forgotPasswordEmailLabel: String
  val forgotPasswordSubmit: String
  val forgotPasswordBackToSignIn: String
  val forgotPasswordSent: String
  val forgotPasswordFailed: String

  // Custom data server wizard
  val customServerTitle: String
  val customServerIntroBody: String
  val customServerStep1: String
  val customServerStep2: String
  val customServerStep3: String
  val customServerFormBody: String
  val customServerUrlLabel: String
  val customServerUrlHint: String
  val customServerPortLabel: String
  val customServerPortHint: String
  val customServerUseOfficial: String
  val customServerNext: String
  val customServerTestingTitle: String
  val customServerTestingBody: String
  val customServerTooLong: String
  val customServerVerifiedTitle: String
  fun customServerVerifiedBody(server: String): String
  val customServerFailedTitle: String
  fun customServerFailedBody(server: String): String
  val customServerSave: String
  val customServerEdit: String
  val customServerClearField: String

  // Settings
  fun settingsVersion(version: String): String
  val settingsTagline: String
  val settingsTitle: String
  val settingsAccountTitle: String
  val settingsAccountSubtitle: String
  val settingsAirBeamsTitle: String
  val settingsAirBeamsSubtitle: String
  val settingsAppTitle: String
  val settingsAppSubtitle: String
  val settingsHelpTitle: String
  val settingsHelpSubtitle: String
  val helpSectionActions: String
  /** Locale sent to the backend for server-translated copy; must match this object's language. */
  val contentLocale: String
  val settingsAccountSectionHeader: String
  // App settings — section headers
  val appSettingsCommunityHeader: String
  val appSettingsUnitsRegionHeader: String
  val appSettingsDisplayHeader: String
  val appSettingsSensorsHeader: String
  val appSettingsNotificationsHeader: String
  val appSettingsSyncHeader: String
  val appSettingsBackendHeader: String
  // App settings — rows (subtitle only where the design shows a supporting line)
  val appSettingCrowdMap: String
  val appSettingCrowdMapSubtitle: String
  val appSettingDisableMapping: String
  val appSettingDisableMappingSubtitle: String
  val appSettingTemperatureUnits: String
  val appSettingRegionalFormats: String
  val appSettingRegionalFormatsSubtitle: String
  val appSettingLanguage: String
  val appSettingMapType: String
  val appSettingDarkMode: String
  val appSettingMicrophoneCalibration: String
  val appSettingMicrophoneCalibrationSubtitle: String
  val appSettingPushNotifications: String
  val appSettingPushNotificationsSubtitle: String
  val appSettingWifiOnlySync: String
  val appSettingWifiOnlySyncSubtitle: String
  val appSettingCustomDataServer: String
  val appSettingCustomDataServerSubtitle: String

  // App settings — trailing values on the picker rows
  val appSettingValueFahrenheit: String
  val appSettingValueCelsius: String
  val appSettingValueRegionUs: String
  val appSettingValueRegionUk: String
  val appSettingValueRegionCentralEuropean: String
  val appSettingValueRegionNordic: String
  val appSettingValueRegionEastAsian: String
  val appSettingValueMapDefault: String
  val appSettingValueMapSatellite: String
  val languageName: String
  // App settings — choice sheets: full unit names plus the worked example under each
  val appSettingFahrenheit: String
  val appSettingFahrenheitDetail: String
  val appSettingCelsius: String
  val appSettingCelsiusDetail: String
  val unitMiles: String
  val unitKilometers: String

  val accountChangeEmail: String
  val accountChangeUsername: String
  val accountChangePassword: String
  val accountCurrentPasswordLabel: String
  val accountNewPasswordLabel: String
  val accountConfirmPasswordLabel: String
  val accountSignOut: String
  val accountDeleteAccount: String
  val accountSave: String
  val accountUpdateFailed: String
  val accountUpdateRefused: String
  val fieldErrorTaken: String
  val fieldErrorBlank: String
  val fieldErrorTooShort: (min: String) -> String
  val fieldErrorTooLong: (max: String) -> String
  val fieldErrorPasswordMismatch: String
  val fieldErrorWrongPassword: String
  val fieldErrorInvalid: String
  val cancel: String
  val done: String
  val deleteAccountConfirmTitle: String
  val deleteAccountConfirmBody: String
  val deleteAccountSendCode: String
  val deleteAccountCodeTitle: String
  fun deleteAccountCodeBody(email: String): String
  val deleteAccountCodeLabel: String
  val deleteAccountCodeInvalid: String
  val deleteAccountResend: String
  // Microphone calibration sheet
  val micLiveReading: String
  val micDecibels: String
  val micCalibrationHint: String
  val micCalibrationOffset: String
  fun micResetOffset(offset: Int): String
  val micDecreaseOffset: String
  val micIncreaseOffset: String
  val micPermissionNeeded: String

  // Accessibility labels for icon-only controls
  val openSettings: String
  val back: String
  val appLogo: String
  // Bottom navigation
  val tabHome: String
  val tabExplore: String
  val tabRecord: String
  val tabFavorites: String
  val tabMyData: String

  // Error state
  val errorTitle: String
  val errorRetry: String

  // Air quality card — heading & empty states
  val airQualityLabel: String
  val noReadingsTitle: String
  val noReadingsBody: String
  val noLocationTitle: String
  val noLocationBody: String
  val turnOnLocation: String

  // Air quality status, keyed by worst-pollutant level (label + description)
  val aqGoodLabel: String
  val aqGoodDescription: String
  val aqModerateLabel: String
  val aqModerateDescription: String
  val aqUnhealthySensitiveLabel: String
  val aqUnhealthySensitiveDescription: String
  val aqUnhealthyLabel: String
  val aqUnhealthyDescription: String
  val aqHazardousLabel: String
  val aqHazardousDescription: String

  // Pollutant names
  val pollutantPm25: String
  val pollutantNo2: String
  val pollutantOzone: String

  // Nearby stations
  val nearbyStationsTitle: String
  val viewMap: String
  val govMonitor: String

  // Distance & relative time — parameterised, hence functions
  fun distanceAway(miles: Double): String
  val ageJustNow: String
  fun ageMinutesAgo(minutes: Long): String
  fun ageHoursAgo(hours: Long): String
  fun ageDaysAgo(days: Long): String

// Explore — search & filters
  val exploreSearchHint: String
  val exploreFilters: String
  val exploreFilterSource: String
  val exploreFilterParameter: String
  val exploreShowAll: String
  val exploreOnlyAirBeam: String
  val exploreOnlyGovernment: String
  val exploreSourceAirBeam: String
  val exploreSourceGovernment: String
  val exploreParticulateMatter: String
  val exploreNitrogenDioxide: String
  val exploreOzone: String
  val exploreTemperature: String
  val exploreHumidity: String
  val exploreClearAll: String
  fun exploreShowResults(n: Int?): String
  fun exploreRemoveFilter(name: String): String
}
