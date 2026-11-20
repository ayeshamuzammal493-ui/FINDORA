package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FindoraLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = ElectricBlueSoft,
    onPrimaryContainer = DeepNavy,
    secondary = DeepNavy,
    onSecondary = PureWhite,
    secondaryContainer = NavyContainer,
    onSecondaryContainer = DeepNavy,
    tertiary = FoundEmerald,
    onTertiary = PureWhite,
    tertiaryContainer = FoundEmeraldSoft,
    onTertiaryContainer = DeepNavy,
    background = PureWhite,
    onBackground = DeepNavy,
    surface = PureWhite,
    onSurface = DeepNavy,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateText,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceVariant,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = LostCrimson,
    onError = PureWhite,
    errorContainer = LostCrimsonSoft,
    onErrorContainer = LostCrimson
)

val FindoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FindoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FindoraLightColorScheme,
        typography = FindoraTypography,
        shapes = FindoraShapes,
        content = content
    )
}
