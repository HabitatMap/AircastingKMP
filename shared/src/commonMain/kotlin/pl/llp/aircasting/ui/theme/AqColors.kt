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

/**
 * The one AQ palette, for both themes and every surface — map dots and pills, badges, card numerals,
 * graph bands and the threshold slider. These are the bright graph-band shades (Figma `720:867`);
 * they replaced the darker light-theme set and the separate dark-theme set (2026-11-18), so the app
 * shows each level in one colour. There is no distinct "hazardous" shade in that set: above "very
 * high" stays the same red, as the graph's top band always did.
 */
internal val AqPalette = AqColors(
  good = Color(0xFF96D788),
  moderate = Color(0xFFFDD961),
  unhealthySensitive = Color(0xFFFCA443),
  unhealthy = Color(0xFFE85E60),
  hazardous = Color(0xFFE85E60),
)

val LocalAqColors = staticCompositionLocalOf { AqPalette }
