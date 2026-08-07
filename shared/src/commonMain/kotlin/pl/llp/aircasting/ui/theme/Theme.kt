package pl.llp.aircasting.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
  primary = Color(0xFF00668A),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFF00B2EF),
  onPrimaryContainer = Color(0xFF004059),
  inversePrimary = Color(0xFF7BD0FF),
  secondary = Color(0xFF006382),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFF0A7DA3),
  onSecondaryContainer = Color(0xFFF9FCFF),
  tertiary = Color(0xFF725C00),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFE0BD3E),
  surfaceTint = Color(0xFF7BD0FF),
  onTertiaryContainer = Color(0xFF5E4C00),
  error = Color(0xFF9C3D3D),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFBB5453),
  onErrorContainer = Color(0xFFFFFBFF),
  background = Color(0xFFF5FAFF),
  onBackground = Color(0xFF171C20),
  surface = Color(0xFFFCF8F8),
  onSurface = Color(0xFF1C1B1B),
  onSurfaceVariant = Color(0xFF44474A),
  surfaceDim = Color(0xFFDDD9D9),
  surfaceBright = Color(0xFFFCF8F8),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF6F3F2),
  surfaceContainer = Color(0xFFF1EDEC),
  surfaceContainerHigh = Color(0xFFEBE7E7),
  surfaceContainerHighest = Color(0xFFE5E2E1),
  outline = Color(0xFF75777B),
  outlineVariant = Color(0xFFC5C6CA),
  inverseSurface = Color(0xFF313030),
  inverseOnSurface = Color(0xFFF4F0EF),
  scrim = Color(0xFF000000),
)

internal val DarkColors = darkColorScheme(
  primary = Color(0xFF7BD0FF),
  onPrimary = Color(0xFF00354A),
  primaryContainer = Color(0xFF00B2EF),
  onPrimaryContainer = Color(0xFF004059),
  inversePrimary = Color(0xFF00668A),
  secondary = Color(0xFF7AD1FB),
  onSecondary = Color(0xFF003547),
  secondaryContainer = Color(0xFF0A7DA3),
  onSecondaryContainer = Color(0xFFF9FCFF),
  tertiary = Color(0xFFFED957),
  onTertiary = Color(0xFF3B2F00),
  tertiaryContainer = Color(0xFFE0BD3E),
  onTertiaryContainer = Color(0xFF5E4C00),
  error = Color(0xFFFFB3B0),
  onError = Color(0xFF611116),
  errorContainer = Color(0xFFDE6F6D),
  onErrorContainer = Color(0xFF54060E),
  background = Color(0xFF121111),
  onBackground = Color(0xFFE5E2E1),
  surface = Color(0xFF141313),
  onSurface = Color(0xFFE5E2E1),
  onSurfaceVariant = Color(0xFFC5C6CA),
  surfaceTint = Color(0xFF7BD0FF),
  surfaceDim = Color(0xFF141313),
  surfaceBright = Color(0xFF3A3939),
  surfaceContainerLowest = Color(0xFF242323),
  surfaceContainerLow = Color(0xFF1C1B1B),
  surfaceContainer = Color(0xFF201F1F),
  surfaceContainerHigh = Color(0xFF2A2A2A),
  surfaceContainerHighest = Color(0xFF353434),
  outline = Color(0xFF8F9194),
  outlineVariant = Color(0xFF44474A),
  inverseSurface = Color(0xFFE5E2E1),
  inverseOnSurface = Color(0xFF313030),
  scrim = Color(0xFF000000),
)

@Composable
fun AircastingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalAqColors provides if (darkTheme) DarkAqColors else LightAqColors) {
    MaterialTheme(
      colorScheme = if (darkTheme) DarkColors else LightColors,
      typography = rememberAircastingTypography(),
    ) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        content = content,
      )
    }
  }
}