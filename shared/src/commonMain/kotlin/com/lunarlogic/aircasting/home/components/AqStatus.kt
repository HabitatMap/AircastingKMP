package com.lunarlogic.aircasting.home.components

import com.lunarlogic.aircasting.domain.MeasurementLevel

/** Overall air-quality status shown on the cards. Derived from the worst pollutant level. */
internal data class AqStatus(val label: String, val description: String)

internal fun MeasurementLevel.aqStatus(): AqStatus = when (this) {
  MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW ->
    AqStatus("Good", "The air outside is clean. A great time to enjoy activities outside.")
  // TODO: copy + illustrations for these come from the other Figma states — placeholder for now.
  MeasurementLevel.MEDIUM ->
    AqStatus("Moderate", "Air quality is acceptable for most people.")
  MeasurementLevel.HIGH ->
    AqStatus("Unhealthy for sensitive groups", "Sensitive groups should limit outdoor exertion.")
  MeasurementLevel.VERY_HIGH ->
    AqStatus("Unhealthy", "Everyone may begin to feel effects. Limit outdoor time.")
  MeasurementLevel.EXTREMELY_HIGH ->
    AqStatus("Hazardous", "Health warning. Avoid outdoor activity.")
}
