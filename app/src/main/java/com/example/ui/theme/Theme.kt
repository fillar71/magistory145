package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MagistoryDarkColorScheme = darkColorScheme(
    primary = MagistoryPrimaryPurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B236E),
    onPrimaryContainer = Color(0xFFE9D8FD),
    secondary = MagistoryElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = MagistorySunsetCoral,
    onTertiary = Color.White,
    background = MagistoryDeepOnyx,
    onBackground = TextWhitePrimary,
    surface = MagistorySurfaceDark,
    onSurface = TextWhitePrimary,
    surfaceVariant = MagistorySurfaceElevated,
    onSurfaceVariant = TextMutedSecondary,
    outline = MagistorySurfaceBorder
)

private val MagistoryLightColorScheme = lightColorScheme(
    primary = MagistoryPrimaryPurple,
    onPrimary = Color.White,
    secondary = MagistoryElectricCyan,
    onSecondary = Color.White,
    tertiary = MagistorySunsetCoral,
    background = Color(0xFF0F0C1B), // Video editors are naturally dark themed for color grading
    surface = Color(0xFF181427),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MagistoryTheme(
    darkTheme: Boolean = true, // Default to sleek studio dark aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MagistoryDarkColorScheme else MagistoryLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for test compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MagistoryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
