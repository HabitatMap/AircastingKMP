package com.lunarlogic.aircasting.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.LanguageTag

/** English copy — the source-of-truth locale. Add `es`/`fr` as sibling values and map them below. */
val EnStrings = Strings(
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
)

/** Every available locale, keyed by BCP-47 tag. Lyricist selects one by the system locale at runtime. */
val AppStrings: Map<LanguageTag, Strings> = mapOf(
  "en" to EnStrings,
)

/**
 * The active locale's copy, readable anywhere in composition via `LocalStrings.current`.
 * Defaults to [EnStrings] so `@Preview`s and tests resolve without an explicit provider;
 * [com.lunarlogic.aircasting.App] overrides it with the system-selected locale at the root.
 */
val LocalStrings = staticCompositionLocalOf { EnStrings }
