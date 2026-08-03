package pl.llp.aircasting.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import pl.llp.aircasting.domain.MeasurementLevel

/** Air-quality level palette — semantic colors with no M3 ColorScheme slot. */
@Immutable
data class AqColors(
  val good: Color,
  val moderate: Color,
  val unhealthySensitive: Color,
  val unhealthy: Color,
  val hazardous: Color,
) {
  fun forLevel(level: MeasurementLevel): Color = when (level) {
    MeasurementLevel.EXTREMELY_LOW, MeasurementLevel.LOW -> good
    MeasurementLevel.MEDIUM -> moderate
    MeasurementLevel.HIGH -> unhealthySensitive
    MeasurementLevel.VERY_HIGH -> unhealthy
    MeasurementLevel.EXTREMELY_HIGH -> hazardous
  }
}

internal val LightAqColors = AqColors(
  good = Color(0xFF006E02),
  moderate = Color(0xFFC9A400),
  unhealthySensitive = Color(0xFFE8720C),
  unhealthy = Color(0xFFD32F2F),
  hazardous = Color(0xFF7B1FA2),
)

internal val DarkAqColors = AqColors(
  good = Color(0xFF6EDC6E),
  moderate = Color(0xFFEBCD4A),
  unhealthySensitive = Color(0xFFFF9F55),
  unhealthy = Color(0xFFFF8A80),
  hazardous = Color(0xFFD69CF0),
)

val LocalAqColors = staticCompositionLocalOf { LightAqColors }
