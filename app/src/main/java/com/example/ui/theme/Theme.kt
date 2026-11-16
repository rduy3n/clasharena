package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClashColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = TextPrimary,
    primaryContainer = Color(0xFF4A190C),
    onPrimaryContainer = FlameOrangeLight,
    secondary = ElectricCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF083945),
    onSecondaryContainer = ElectricCyan,
    tertiary = UltiGold,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    error = CrimsonHp,
    onError = TextPrimary
)

private val ClashShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun ClashArenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClashColorScheme,
        typography = ClashTypography,
        shapes = ClashShapes,
        content = content
    )
}
