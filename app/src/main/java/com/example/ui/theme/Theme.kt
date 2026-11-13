package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpinMintDarkColorScheme = darkColorScheme(
    primary = NeonGold,
    onPrimary = VoidObsidian,
    primaryContainer = DeepGold,
    onPrimaryContainer = Color(0xFFFFF3CC),
    secondary = EmeraldMint,
    onSecondary = VoidObsidian,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = ElectricViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = CardSlateElevated,
    onTertiaryContainer = TextPrimary,
    background = VoidObsidian,
    onBackground = TextPrimary,
    surface = DeepIndigo,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextSecondary,
    outline = BorderViolet,
    error = RoseAlert,
    onError = TextPrimary
)

val SpinMintShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SpinMintTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpinMintDarkColorScheme,
        typography = SpinMintTypography,
        shapes = SpinMintShapes,
        content = content
    )
}
