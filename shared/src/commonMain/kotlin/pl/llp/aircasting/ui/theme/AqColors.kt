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

/**
 * Darker AQ shades for level *text* on light surfaces (the Home status line; badges have their
 * own [AqBadgeColors]) — the bright
 * [AqPalette] is well under 3:1 on white, so the designs letter level text in these instead (Figma:
 * `Extended Colors/Green`, `Custom/Badge/On Moderate Container`, `Custom/Badge/On Unhealthy
 * Container`; the orange has no Figma token and keeps the app's earlier darker shade). Fills —
 * dots, borders, graph bands — stay on [AqPalette]. Dark theme letters text in
 * [AqPalette] itself, which already clears 3:1 on the dark card.
 */
internal val AqTextPaletteLight = AqColors(
  good = Color(0xFF006E02),
  moderate = Color(0xFF86711D),
  unhealthySensitive = Color(0xFFE8720C),
  unhealthy = Color(0xFFC62828),
  hazardous = Color(0xFFC62828),
)

/** Background ([container]) and label ([content]) of the level pill (Figma "StatusBadge"). */
@Immutable
data class AqBadgeColors(val container: AqColors, val content: AqColors)

/**
 * Light-theme badge, straight from Figma (`859:7246`, `1145:29607`, `1116:18429`): `Extended
 * Colors/Green Container` / `On Green Container`, `Custom/Badge/Moderate Container` / `On Moderate
 * Container`, `Custom/Badge/Unhealthy Container` / `On Unhealthy Container`. Figma has no orange
 * badge: its container follows the same Material "50" step (Orange 50), its label is a dark orange
 * picked to read like the others (4.6:1 on that container).
 */
internal val AqBadgePaletteLight = AqBadgeColors(
  container = AqColors(
    good = Color(0xFFE8F5E9),
    moderate = Color(0xFFFFF3CD),
    unhealthySensitive = Color(0xFFFFF3E0),
    unhealthy = Color(0xFFFFEBEE),
    hazardous = Color(0xFFFFEBEE),
  ),
  content = AqColors(
    good = Color(0xFF003E01),
    moderate = Color(0xFF86711D),
    unhealthySensitive = Color(0xFFB45309),
    unhealthy = Color(0xFFC62828),
    hazardous = Color(0xFFC62828),
  ),
)

/** Dark theme has no badge design: a faint tint of the bright level colour, lettered in it. */
internal val AqBadgePaletteDark = AqBadgeColors(
  container = with(AqPalette) {
    AqColors(
      good.copy(alpha = 0.12f), moderate.copy(alpha = 0.12f), unhealthySensitive.copy(alpha = 0.12f),
      unhealthy.copy(alpha = 0.12f), hazardous.copy(alpha = 0.12f),
    )
  },
  content = AqPalette,
)

val LocalAqColors = staticCompositionLocalOf { AqPalette }

val LocalAqBadgeColors = staticCompositionLocalOf { AqBadgePaletteLight }

/** AQ colours for text; see [AqTextPaletteLight]. */
val LocalAqTextColors = staticCompositionLocalOf { AqTextPaletteLight }
