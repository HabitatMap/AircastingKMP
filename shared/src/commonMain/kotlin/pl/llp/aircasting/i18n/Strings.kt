package pl.llp.aircasting.i18n

/**
 * All user-facing copy for the app, as plain Kotlin — one instance per locale.
 *
 * Because the copy is Kotlin (not XML), formatting is trivial:
 *  - line breaks: put `\n` in the string, or use a `"""triple-quoted"""` multi-line literal;
 *  - parameterised copy: expose it as a lambda so the call site reads like a function
 *    (e.g. `strings.ageMinutesAgo(3)`), with full type-safety on the arguments.
 *
 * Add a new language by creating a sibling `EsStrings` / `FrStrings` value and registering it
 * in [AppStrings]. The field set here is the single source of truth every locale must fill in.
 */
data class Strings(
  // Onboarding — TODO(design): Figma still has lorem ipsum, this copy needs sign-off
  val onboardingMeasureTitle: String,
  val onboardingMeasureBody: String,
  val onboardingMapTitle: String,
  val onboardingMapBody: String,
  val onboardingShareTitle: String,
  val onboardingShareBody: String,
  val onboardingNext: String,
  val onboardingSkip: String,
  // Auth — headers
  val authWelcomeTitle: String,
  val authWelcomeSubtitle: String,
  val authSignInSheetSubtitle: String,
  val authSignUpSheetSubtitle: String,
  // Auth — tabs & actions
  val authTabSignIn: String,
  val authTabSignUp: String,
  val authSignInAction: String,
  val authSignUpAction: String,
  val authNoAccountPrompt: String,
  val authHaveAccountPrompt: String,
  // Auth — field labels (they float above the input, so they read as instructions)
  val authLoginLabel: String,
  val authEmailLabel: String,
  val authUsernameLabel: String,
  val authPasswordLabel: String,
  val authNewPasswordLabel: String,
  // Auth — errors the backend can't phrase for us
  val authInvalidCredentials: String,
  val authUnexpectedError: String,
  // Auth — accessibility labels for icon-only controls
  val authShowPassword: String,
  val authHidePassword: String,
  val authClearField: String,
  // Forgot password
  val authForgotPassword: String,
  val forgotPasswordTitle: String,
  val forgotPasswordBody: String,
  val forgotPasswordEmailLabel: String,
  val forgotPasswordSubmit: String,
  val forgotPasswordBackToSignIn: String,
  val forgotPasswordSent: String,
  val forgotPasswordFailed: String,

  // Custom data server wizard
  val customServerTitle: String,
  val customServerIntroBody: String,
  val customServerStep1: String,
  val customServerStep2: String,
  val customServerStep3: String,
  val customServerFormBody: String,
  val customServerUrlLabel: String,
  val customServerUrlHint: String,
  val customServerPortLabel: String,
  val customServerPortHint: String,
  val customServerUseOfficial: String,
  val customServerNext: String,
  val customServerTestingTitle: String,
  val customServerTestingBody: String,
  val customServerTooLong: String,
  val customServerVerifiedTitle: String,
  val customServerVerifiedBody: (server: String) -> String,
  val customServerFailedTitle: String,
  val customServerFailedBody: (server: String) -> String,
  val customServerSave: String,
  val customServerEdit: String,
  val customServerClearField: String,

  // Settings
  val settingsVersion: (version: String) -> String,
  val settingsTagline: String,
  val settingsTitle: String,
  val settingsAccountTitle: String,
  val settingsAccountSubtitle: String,
  val settingsAirBeamsTitle: String,
  val settingsAirBeamsSubtitle: String,
  val settingsAppTitle: String,
  val settingsAppSubtitle: String,
  val settingsHelpTitle: String,
  val settingsHelpSubtitle: String,
  val settingsAccountSectionHeader: String,
  // App settings — section headers
  val appSettingsCommunityHeader: String,
  val appSettingsUnitsRegionHeader: String,
  val appSettingsDisplayHeader: String,
  val appSettingsSensorsHeader: String,
  val appSettingsNotificationsHeader: String,
  val appSettingsSyncHeader: String,
  val appSettingsBackendHeader: String,
  // App settings — rows (subtitle only where the design shows a supporting line)
  val appSettingCrowdMap: String,
  val appSettingCrowdMapSubtitle: String,
  val appSettingDisableMapping: String,
  val appSettingDisableMappingSubtitle: String,
  val appSettingTemperatureUnits: String,
  val appSettingRegionalFormats: String,
  val appSettingRegionalFormatsSubtitle: String,
  val appSettingLanguage: String,
  val appSettingMapType: String,
  val appSettingDarkMode: String,
  val appSettingMicrophoneCalibration: String,
  val appSettingMicrophoneCalibrationSubtitle: String,
  val appSettingPushNotifications: String,
  val appSettingPushNotificationsSubtitle: String,
  val appSettingWifiOnlySync: String,
  val appSettingWifiOnlySyncSubtitle: String,
  val appSettingCustomDataServer: String,
  val appSettingCustomDataServerSubtitle: String,

  // App settings — trailing values on the picker rows
  val appSettingValueFahrenheit: String,
  val appSettingValueCelsius: String,
  val appSettingValueRegionUs: String,
  val appSettingValueRegionUk: String,
  val appSettingValueRegionCentralEuropean: String,
  val appSettingValueRegionNordic: String,
  val appSettingValueRegionEastAsian: String,
  val appSettingValueMapDefault: String,
  val appSettingValueMapSatellite: String,
  val languageName: String,
  // App settings — choice sheets: full unit names plus the worked example under each
  val appSettingFahrenheit: String,
  val appSettingFahrenheitDetail: String,
  val appSettingCelsius: String,
  val appSettingCelsiusDetail: String,
  val unitMiles: String,
  val unitKilometers: String,

  val accountChangeEmail: String,
  val accountChangeUsername: String,
  val accountResetPassword: String,
  val accountSignOut: String,
  val accountDeleteAccount: String,
  val cancel: String,
  val done: String,
  val deleteAccountConfirmTitle: String,
  val deleteAccountConfirmBody: String,
  val deleteAccountSendCode: String,
  val deleteAccountCodeTitle: String,
  val deleteAccountCodeBody: (email: String) -> String,
  val deleteAccountCodeLabel: String,
  val deleteAccountCodeInvalid: String,
  val deleteAccountResend: String,
  // Microphone calibration sheet
  val micLiveReading: String,
  val micDecibels: String,
  val micCalibrationHint: String,
  val micCalibrationOffset: String,
  val micResetOffset: (offset: Int) -> String,
  val micDecreaseOffset: String,
  val micIncreaseOffset: String,
  val micPermissionNeeded: String,

  // Accessibility labels for icon-only controls
  val openSettings: String,
  val back: String,
  val appLogo: String,
  // Bottom navigation
  val tabHome: String,
  val tabExplore: String,
  val tabRecord: String,
  val tabFavorites: String,
  val tabMyData: String,

  // Error state
  val errorTitle: String,
  val errorRetry: String,

  // Air quality card — heading & empty states
  val airQualityLabel: String,
  val noReadingsTitle: String,
  val noReadingsBody: String,
  val noLocationTitle: String,
  val noLocationBody: String,
  val turnOnLocation: String,

  // Air quality status, keyed by worst-pollutant level (label + description)
  val aqGoodLabel: String,
  val aqGoodDescription: String,
  val aqModerateLabel: String,
  val aqModerateDescription: String,
  val aqUnhealthySensitiveLabel: String,
  val aqUnhealthySensitiveDescription: String,
  val aqUnhealthyLabel: String,
  val aqUnhealthyDescription: String,
  val aqHazardousLabel: String,
  val aqHazardousDescription: String,

  // Pollutant names
  val pollutantPm25: String,
  val pollutantNo2: String,
  val pollutantOzone: String,

  // Nearby stations
  val nearbyStationsTitle: String,
  val viewMap: String,
  val govMonitor: String,

  // Distance & relative time — parameterised, hence lambdas
  val distanceAway: (miles: Double) -> String,
  val ageJustNow: String,
  val ageMinutesAgo: (minutes: Long) -> String,
  val ageHoursAgo: (hours: Long) -> String,
  val ageDaysAgo: (days: Long) -> String,
)
