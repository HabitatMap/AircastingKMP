package pl.llp.aircasting.ui.theme

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.roboto_bold
import aircasting.shared.generated.resources.roboto_medium
import aircasting.shared.generated.resources.roboto_regular
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

@Composable
private fun rememberRobotoFamily(): FontFamily {
  val regular = Font(Res.font.roboto_regular, FontWeight.Normal)
  val medium = Font(Res.font.roboto_medium, FontWeight.Medium)
  val bold = Font(Res.font.roboto_bold, FontWeight.Bold)
  return remember(regular, medium, bold) { FontFamily(regular, medium, bold) }
}

@Composable
internal fun rememberAircastingTypography(): Typography {
  val roboto = rememberRobotoFamily()
  return remember(roboto) { aircastingTypography(roboto) }
}

internal fun aircastingTypography(fontFamily: FontFamily): Typography {
  fun style(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.toFloat().sp,
  )

  val regular = FontWeight.Normal
  val medium = FontWeight.Medium
  val semiBold = FontWeight.SemiBold

  return Typography(
    displayLarge = style(57, 64, -0.25, regular),
    displayMedium = style(45, 52, 0.0, regular),
    displaySmall = style(36, 44, 0.0, regular),
    headlineLarge = style(32, 40, 0.0, regular),
    headlineMedium = style(28, 36, 0.0, regular),
    headlineSmall = style(24, 32, 0.0, regular),
    titleLarge = style(22, 28, 0.0, regular),
    titleMedium = style(16, 24, 0.15, medium),
    titleSmall = style(14, 20, 0.1, medium),
    bodyLarge = style(16, 24, 0.5, regular),
    bodyMedium = style(14, 20, 0.25, regular),
    bodySmall = style(12, 16, 0.4, regular),
    labelLarge = style(14, 20, 0.1, medium),
    labelMedium = style(12, 16, 0.5, medium),
    labelSmall = style(11, 16, 0.5, medium),
    // Emphasized: identical metrics, one step heavier. Used by M3 expressive components and
    // available to us for the same text at higher emphasis without a reflow.
    displayLargeEmphasized = style(57, 64, -0.25, medium),
    displayMediumEmphasized = style(45, 52, 0.0, medium),
    displaySmallEmphasized = style(36, 44, 0.0, medium),
    headlineLargeEmphasized = style(32, 40, 0.0, medium),
    headlineMediumEmphasized = style(28, 36, 0.0, medium),
    headlineSmallEmphasized = style(24, 32, 0.0, medium),
    titleLargeEmphasized = style(22, 28, 0.0, medium),
    titleMediumEmphasized = style(16, 24, 0.15, semiBold),
    titleSmallEmphasized = style(14, 20, 0.1, semiBold),
    bodyLargeEmphasized = style(16, 24, 0.5, medium),
    bodyMediumEmphasized = style(14, 20, 0.25, medium),
    bodySmallEmphasized = style(12, 16, 0.4, medium),
    labelLargeEmphasized = style(14, 20, 0.1, semiBold),
    labelMediumEmphasized = style(12, 16, 0.5, semiBold),
    labelSmallEmphasized = style(11, 16, 0.5, semiBold),
  )
}
