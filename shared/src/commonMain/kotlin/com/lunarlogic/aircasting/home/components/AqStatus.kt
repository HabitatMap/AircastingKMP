package com.lunarlogic.aircasting.home.components

import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.i18n.Strings

/** Overall air-quality status shown on the cards. Derived from the worst pollutant level. */
internal data class AqStatus(val label: String, val description: String)

internal fun MeasurementLevel.aqStatus(strings: Strings): AqStatus = when (this) {
  MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW ->
    AqStatus(strings.aqGoodLabel, strings.aqGoodDescription)
  // TODO: illustrations for these come from the other Figma states — copy is now localised.
  MeasurementLevel.MEDIUM ->
    AqStatus(strings.aqModerateLabel, strings.aqModerateDescription)
  MeasurementLevel.HIGH ->
    AqStatus(strings.aqUnhealthySensitiveLabel, strings.aqUnhealthySensitiveDescription)
  MeasurementLevel.VERY_HIGH ->
    AqStatus(strings.aqUnhealthyLabel, strings.aqUnhealthyDescription)
  MeasurementLevel.EXTREMELY_HIGH ->
    AqStatus(strings.aqHazardousLabel, strings.aqHazardousDescription)
}
