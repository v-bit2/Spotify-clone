package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpotificColorScheme = darkColorScheme(
    primary = SpotificBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SpotificBlueSecondary,
    onPrimaryContainer = Color.White,
    secondary = SpotificBlueSecondary,
    onSecondary = Color.White,
    tertiary = SpotificBluePrimary,
    background = SpotificDarkBg,
    onBackground = SpotificTextPrimary,
    surface = SpotificCardSurface,
    onSurface = SpotificTextPrimary,
    surfaceVariant = SpotificDarkBgSecondary,
    onSurfaceVariant = SpotificTextSecondary,
    outline = SpotificCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpotificColorScheme,
        typography = Typography,
        content = content
    )
}
