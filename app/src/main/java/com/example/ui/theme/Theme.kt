package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MehfilDarkColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = TextPrimary,
    secondary = ElectricViolet,
    onSecondary = TextPrimary,
    tertiary = RoyalGold,
    onTertiary = StageNightBg,
    background = StageNightBg,
    onBackground = TextPrimary,
    surface = VelvetPlumSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassCardSurface,
    onSurfaceVariant = TextSecondary,
    error = MicMutedRed,
    onError = TextPrimary,
    outline = DividerSubtle
)

private val MehfilShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MehfilPartyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MehfilDarkColorScheme,
        typography = Typography,
        shapes = MehfilShapes,
        content = content
    )
}
