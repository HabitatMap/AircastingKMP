package pl.llp.aircasting.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Thirty hand-transcribed styles is the same paste-in-the-wrong-slot hazard as the color scheme,
 * so this pins the invariants a slip breaks — not every number (that would just restate the
 * implementation), but the ones that make the scale a scale.
 */
class TypographyTest {

  private val family = FontFamily.Monospace // stand-in for Roboto: the test never loads a font
  private val typography = aircastingTypography(family)

  private val baseStyles get() = with(typography) {
    mapOf(
      "displayLarge" to displayLarge, "displayMedium" to displayMedium, "displaySmall" to displaySmall,
      "headlineLarge" to headlineLarge, "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
      "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
      "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
      "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall,
    )
  }

  private val emphasizedStyles get() = with(typography) {
    mapOf(
      "displayLarge" to displayLargeEmphasized, "displayMedium" to displayMediumEmphasized,
      "displaySmall" to displaySmallEmphasized, "headlineLarge" to headlineLargeEmphasized,
      "headlineMedium" to headlineMediumEmphasized, "headlineSmall" to headlineSmallEmphasized,
      "titleLarge" to titleLargeEmphasized, "titleMedium" to titleMediumEmphasized,
      "titleSmall" to titleSmallEmphasized, "bodyLarge" to bodyLargeEmphasized,
      "bodyMedium" to bodyMediumEmphasized, "bodySmall" to bodySmallEmphasized,
      "labelLarge" to labelLargeEmphasized, "labelMedium" to labelMediumEmphasized,
      "labelSmall" to labelSmallEmphasized,
    )
  }

  @Test
  fun `every style carries the app font family`() {
    // The whole point of bundling Roboto is that iOS doesn't fall back to SF Pro and Samsung
    // doesn't substitute SamsungOne. A slot left unset silently reverts to the platform face,
    // which looks fine on a Pixel and wrong everywhere else — so assert all thirty.
    (baseStyles + emphasizedStyles.mapKeys { "${it.key}Emphasized" }).forEach { (name, style) ->
      assertEquals(family, style.fontFamily, "$name uses the platform default font")
    }
  }

  @Test
  fun `emphasized styles differ from their base only in weight`() {
    // M3's emphasized set is the same metrics at a heavier weight. If a size or tracking drifts
    // between the pair, switching a Text to the emphasized style would reflow the layout.
    emphasizedStyles.forEach { (name, emphasized) ->
      val base = baseStyles.getValue(name)
      assertEquals(base.fontSize, emphasized.fontSize, "${name}Emphasized size")
      assertEquals(base.lineHeight, emphasized.lineHeight, "${name}Emphasized line height")
      assertEquals(base.letterSpacing, emphasized.letterSpacing, "${name}Emphasized tracking")
      assertTrue(
        emphasized.fontWeight!!.weight > base.fontWeight!!.weight,
        "${name}Emphasized is not heavier than $name",
      )
    }
  }

  @Test
  fun `line height always clears the font size`() {
    // Catches a size and line-height swapped in a paste: 57sp text in a 24sp box clips ascenders.
    baseStyles.forEach { (name, style) ->
      assertTrue(
        style.lineHeight.value > style.fontSize.value,
        "$name line height ${style.lineHeight} <= size ${style.fontSize}",
      )
    }
  }

  @Test
  fun `each role descends from large to small`() {
    // A style pasted into the wrong slot within its role shows up here and nowhere else.
    listOf(
      Triple(typography.displayLarge, typography.displayMedium, typography.displaySmall),
      Triple(typography.headlineLarge, typography.headlineMedium, typography.headlineSmall),
      Triple(typography.titleLarge, typography.titleMedium, typography.titleSmall),
      Triple(typography.bodyLarge, typography.bodyMedium, typography.bodySmall),
      Triple(typography.labelLarge, typography.labelMedium, typography.labelSmall),
    ).forEach { (large, medium, small) ->
      assertTrue(large.fontSize > medium.fontSize, "large ${large.fontSize} !> medium ${medium.fontSize}")
      assertTrue(medium.fontSize > small.fontSize, "medium ${medium.fontSize} !> small ${small.fontSize}")
    }
  }

  @Test
  fun `anchor styles match the Figma type scale`() {
    // Three corners of the table, pinned literally: the largest, the workhorse body style, and the
    // smallest. Together with the invariants above, a drifted transcription can't pass unnoticed.
    typography.displayLarge.assertIs(57, 64, -0.25, FontWeight.Normal)
    typography.bodyLarge.assertIs(16, 24, 0.5, FontWeight.Normal)
    typography.labelSmall.assertIs(11, 16, 0.5, FontWeight.Medium)
  }

  private fun TextStyle.assertIs(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight) {
    assertEquals(size.sp, fontSize)
    assertEquals(lineHeight.sp, this.lineHeight)
    assertEquals(tracking.toFloat().sp, letterSpacing)
    assertEquals(weight, fontWeight)
  }
}
