package pl.llp.aircasting.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import pl.llp.aircasting.domain.MeasurementLevel
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Hand-transcribing ~35 hex values per scheme is exactly where a paste lands in the wrong slot.
 * A swapped on-color is invisible in review and glaring on device, so pin the one property that
 * catches it: every foreground stays legible on its own background.
 *
 * 3:1 is the WCAG AA floor for large text and UI components — the right bar for a design-system
 * palette (titles, icons, chip labels), rather than the 4.5:1 body-text bar the brand cyan
 * container would never clear.
 */
class ColorSchemeTest {

  @Test
  fun `light scheme foregrounds are legible on their backgrounds`() = assertLegible(LightColors)

  @Test
  fun `dark scheme foregrounds are legible on their backgrounds`() = assertLegible(DarkColors)

  @Test
  fun `cards read as raised above the page in both schemes`() {
    // surfaceContainerLowest is the app's card and nav-bar fill, and Theme.kt deliberately breaks
    // the ladder's tone ordering in dark to keep it that way. This is what makes that deliberate
    // rather than a typo: whatever tone the role holds, it must stay above `background`, or cards
    // sink into the page — which is exactly what Figma's #0E0E0E did.
    //
    // The ratio floor only applies to dark. Light gets its separation from the card's drop shadow
    // (page→card is only 1.04:1 there); in dark the shadow contributes nothing, so tone is all
    // there is. Card vs `surface` is also asserted — `surface` is the recessed reading tile inside
    // a station card, so it has to stay below the card in both schemes.
    assertRaised("light", LightColors, minRatio = 1.0)
    assertRaised("dark", DarkColors, minRatio = 1.15)
  }

  @Test
  fun `dark air quality colors are legible on their card`() {
    // These are read as a numeral, a border and a dot — always against the card, never the page.
    // Light is deliberately not asserted: moderate #C9A400 on white is 2.4:1, a real pre-existing
    // issue, but fixing it means changing a color the designer specified.
    MeasurementLevel.entries.forEach { level ->
      val ratio = contrastRatio(DarkAqColors.forLevel(level), DarkColors.surfaceContainerLowest)
      assertTrue(ratio >= 3.0, "dark $level contrast ${ratio.format()} < 3.0:1")
    }
  }

  private fun assertRaised(name: String, scheme: ColorScheme, minRatio: Double) = with(scheme) {
    // The card is the lightest of the three in both schemes. `surface` vs `background` is
    // deliberately *not* asserted: light's blue page tint (#F5FAFF) is lighter than the warm
    // off-white tile (#FCF8F8) while dark's neutral page is darker than its tile, so that
    // ordering flips between schemes and isn't an invariant.
    assertTrue(
      surfaceContainerLowest.luminance() > surface.luminance(),
      "$name: surface $surface must sit below the card $surfaceContainerLowest",
    )
    assertTrue(
      surfaceContainerLowest.luminance() > background.luminance(),
      "$name: page $background must sit below the card $surfaceContainerLowest",
    )
    val ratio = contrastRatio(surfaceContainerLowest, background)
    assertTrue(ratio >= minRatio, "$name: card/page contrast ${ratio.format()} < $minRatio:1")
  }

  private fun assertLegible(scheme: ColorScheme) = with(scheme) {
    listOf(
      "onBackground/background" to (onBackground to background),
      "onSurface/surface" to (onSurface to surface),
      "onSurfaceVariant/surface" to (onSurfaceVariant to surface),
      "onSurface/surfaceContainerLowest" to (onSurface to surfaceContainerLowest),
      "onSurface/surfaceContainerHighest" to (onSurface to surfaceContainerHighest),
      "onPrimary/primary" to (onPrimary to primary),
      "onPrimaryContainer/primaryContainer" to (onPrimaryContainer to primaryContainer),
      "onSecondary/secondary" to (onSecondary to secondary),
      "onSecondaryContainer/secondaryContainer" to (onSecondaryContainer to secondaryContainer),
      "onTertiary/tertiary" to (onTertiary to tertiary),
      "onTertiaryContainer/tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
      "onError/error" to (onError to error),
      "onErrorContainer/errorContainer" to (onErrorContainer to errorContainer),
    ).forEach { (name, pair) ->
      val ratio = contrastRatio(pair.first, pair.second)
      assertTrue(ratio >= 3.0, "$name contrast ${ratio.format()} < 3.0:1")
    }
  }
}

/** WCAG 2.x relative luminance + contrast ratio. Pure math, so it runs in commonTest. */
private fun contrastRatio(a: Color, b: Color): Double {
  val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
  return (hi + 0.05) / (lo + 0.05)
}

private fun Color.luminance(): Double {
  fun channel(c: Float): Double =
    if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
  return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}

private fun Double.format(): String = (this * 100).toInt().let { "${it / 100}.${it % 100}" }
