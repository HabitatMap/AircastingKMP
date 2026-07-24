package com.lunarlogic.aircasting.home.components

import com.lunarlogic.aircasting.domain.Pollutant
import kotlin.math.round

internal val Pollutant.label: String
  get() = when (this) {
    Pollutant.PM25 -> "PM 2.5"
    Pollutant.NO2 -> "NO₂"
    Pollutant.OZONE -> "Ozone"
  }

internal fun distanceLabel(meters: Double): String {
  val miles = round(meters / 1609.344 * 10) / 10.0
  return "$miles mile away"
}
