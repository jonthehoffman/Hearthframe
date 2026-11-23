package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HearthLightColorScheme =
  lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OnTerracotta,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = SageSecondary,
    onSecondary = OnSage,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = CreamBackground,
    onBackground = EspressoOnBackground,
    surface = CreamBackground,
    onSurface = EspressoOnBackground,
    surfaceVariant = LinenSurface,
    onSurfaceVariant = WalnutMuted,
    surfaceContainerLowest = CreamBackground,
    surfaceContainerLow = LinenSurfaceLow,
    surfaceContainer = LinenSurface,
    surfaceContainerHigh = LinenSurfaceHigh,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
  )

private val HearthDarkColorScheme =
  darkColorScheme(
    primary = EmberDarkPrimary,
    onPrimary = EmberDarkOnPrimary,
    primaryContainer = EmberDarkContainer,
    onPrimaryContainer = EmberDarkOnContainer,
    secondary = SageDarkSecondary,
    onSecondary = SageDarkOnSecondary,
    secondaryContainer = SageDarkContainer,
    onSecondaryContainer = SageDarkOnContainer,
    tertiary = CandleDarkTertiary,
    onTertiary = CandleDarkOnTertiary,
    tertiaryContainer = CandleDarkContainer,
    onTertiaryContainer = CandleDarkOnContainer,
    background = HearthDarkBackground,
    onBackground = HearthDarkOnSurface,
    surface = HearthDarkBackground,
    onSurface = HearthDarkOnSurface,
    surfaceVariant = HearthDarkSurface,
    onSurfaceVariant = HearthDarkMuted,
    surfaceContainerLowest = HearthDarkBackground,
    surfaceContainerLow = HearthDarkSurfaceLow,
    surfaceContainer = HearthDarkSurface,
    surfaceContainerHigh = HearthDarkSurfaceHigh,
    outline = HearthDarkOutline,
  )

val HearthShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun HearthframeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) HearthDarkColorScheme else HearthLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = HearthTypography,
    shapes = HearthShapes,
    content = content,
  )
}
