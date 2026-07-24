package com.lunarlogic.aircasting.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.lunarlogic.aircasting.domain.MeasurementLevel

/** Air-quality level palette — semantic colors with no M3 ColorScheme slot. */
@Immutable
data class AqColors(
  val good: Color = Color(0xFF006E02),
  val moderate: Color = Color(0xFFC9A400),
  val unhealthySensitive: Color = Color(0xFFE8720C),
  val unhealthy: Color = Color(0xFFD32F2F),
  val hazardous: Color = Color(0xFF7B1FA2),
) {
  fun forLevel(level: MeasurementLevel): Color = when (level) {
    MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW -> good
    MeasurementLevel.MEDIUM -> moderate
    MeasurementLevel.HIGH -> unhealthySensitive
    MeasurementLevel.VERY_HIGH -> unhealthy
    MeasurementLevel.EXTREMELY_HIGH -> hazardous
  }
}

val LocalAqColors = staticCompositionLocalOf { AqColors() }