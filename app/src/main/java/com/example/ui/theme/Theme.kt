package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LauncherDarkColorScheme = darkColorScheme(
    primary = WbGoldPrimary,
    onPrimary = WbGoldOnPrimary,
    primaryContainer = WbGoldContainer,
    onPrimaryContainer = WbGoldOnContainer,
    secondary = WbGreenSecondary,
    onSecondary = WbGreenOnSecondary,
    secondaryContainer = WbGreenContainer,
    onSecondaryContainer = WbGreenOnContainer,
    background = WbSoilBackground,
    onBackground = WbSoilOnBackground,
    surface = WbPanelSurface,
    onSurface = WbPanelOnSurface,
    surfaceVariant = WbPanelVariant,
    onSurfaceVariant = WbPanelOnVariant,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent launcher theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LauncherDarkColorScheme,
        typography = Typography,
        content = content
    )
}
