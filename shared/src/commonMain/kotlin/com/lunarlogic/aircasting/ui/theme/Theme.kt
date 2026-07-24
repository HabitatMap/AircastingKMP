package com.lunarlogic.aircasting.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
  primary = Color(0xFF00668A),
  secondary = Color(0xFF006382),
  background = Color(0xFFF5FAFF),
  onBackground = Color(0xFF171C20),
  surface = Color(0xFFFCF8F8),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerHighest = Color(0xFFE5E2E1),
  onSurface = Color(0xFF1D1B20),
  onSurfaceVariant = Color(0xFF44474A),
  outline = Color(0xFF75777B),
  outlineVariant = Color(0xFFCAC4D0),
  primaryContainer = Color(0xFF00B2EF),
  onPrimaryContainer = Color(0xFF004059),
)

@Composable
fun AircastingTheme(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalAqColors provides AqColors()) {
    MaterialTheme(colorScheme = LightColors, content = content)
  }
}