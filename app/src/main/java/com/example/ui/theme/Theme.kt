package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NexoraDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = VioletNeon,
    onSecondary = OnVioletNeon,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletNeonLight,
    tertiary = EmeraldBullish,
    onTertiary = DeepNavyBackground,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldBullish,
    background = DeepNavyBackground,
    onBackground = PureWhite,
    surface = NavySurface,
    onSurface = PureWhite,
    surfaceVariant = NavySurfaceContainer,
    onSurfaceVariant = SoftSlateText,
    surfaceContainerLowest = DeepNavyBackground,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavySurfaceContainer,
    surfaceContainerHigh = NavySurfaceHigh,
    surfaceContainerHighest = NavyBorder,
    outline = NavyBorder,
    outlineVariant = NavySurfaceHigh,
    error = CrimsonBearish,
    onError = PureWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonBearish
)

val NexoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NexoraAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexoraDarkColorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
