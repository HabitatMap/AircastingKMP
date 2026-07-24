package com.lunarlogic.aircasting.i18n

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
