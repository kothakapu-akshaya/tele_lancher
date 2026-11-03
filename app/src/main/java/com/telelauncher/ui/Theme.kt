package com.telelauncher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

// Dark entertainment dashboard color scheme (navy/black background, purple accent)
private val DarkTeleLauncherColorScheme = darkColorScheme(
    background = Color(0xFF0B0E14),
    surface = Color(0xFF121722),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF3700B3),
    onSecondary = Color(0xFFFFFFFF),
    error = Color(0xFFCF6679),
    onError = Color(0xFFFFFFFF)
)

private val LightTeleLauncherColorScheme = lightColorScheme(
    background = Color(0xFF0B0E14),
    surface = Color(0xFF121722),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF3700B3),
    onSecondary = Color(0xFFFFFFFF),
    error = Color(0xFFCF6679),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun TeleLauncherTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    SideEffect { /* no-op; theme selected below */ }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkTeleLauncherColorScheme else LightTeleLauncherColorScheme,
        typography = androidx.compose.material3.MaterialTheme.typography,
        shapes = androidx.compose.material3.MaterialTheme.shapes,
        content = content
    )
}
