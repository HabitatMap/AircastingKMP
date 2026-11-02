package pl.llp.aircasting.home.components

import pl.llp.aircasting.domain.Pollutant
import pl.llp.aircasting.i18n.Strings
import kotlin.math.round

internal fun Pollutant.label(strings: Strings): String = when (this) {
  Pollutant.PM25 -> strings.pollutantPm25
  Pollutant.NO2 -> strings.pollutantNo2
  Pollutant.OZONE -> strings.pollutantOzone
}

internal fun distanceLabel(meters: Double, strings: Strings): String {
  val miles = round(meters / 1609.344 * 10) / 10.0
  return strings.distanceAway(miles)
}
