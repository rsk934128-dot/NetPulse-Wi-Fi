package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPulse,
    onPrimary = TelemetryDeepNavy,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanPulse,
    secondary = EmeraldSignal,
    onSecondary = TelemetryDeepNavy,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldSignal,
    tertiary = VioletSpectrum,
    onTertiary = TelemetryDeepNavy,
    background = TelemetryDeepNavy,
    onBackground = TextPrimaryDark,
    surface = TelemetrySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = TelemetrySurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = TelemetrySurface,
    surfaceContainer = TelemetrySurfaceHigh,
    surfaceContainerHigh = TelemetrySurfaceHighest,
    outline = TelemetryBorder,
    outlineVariant = TelemetryBorder.copy(alpha = 0.55f),
    error = CoralAlert,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = CoralAlert,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF0E4A5C),
    secondary = EmeraldLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceHigh,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.6f),
    error = CoralLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
  )

val NetPulseShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun NetPulseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = NetPulseShapes,
    content = content,
  )
}
