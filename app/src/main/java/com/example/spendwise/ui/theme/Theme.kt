package com.example.spendwise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom colors
val DarkNavy = Color(0xFF000814)
val Navy = Color(0xFF001D3D)
val LightNavy = Color(0xFF003566)
val Gold = Color(0xFFFFC300)
val LightGold = Color(0xFFFFD60A)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkNavy,
    secondary = LightGold,
    onSecondary = DarkNavy,
    tertiary = LightNavy,
    onTertiary = Gold,
    background = DarkNavy,
    onBackground = Gold,
    surface = Navy,
    onSurface = Gold,
    surfaceVariant = LightNavy,
    onSurfaceVariant = LightGold,
    error = Color(0xFFFF5252),
    onError = DarkNavy
)

private val LightColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = DarkNavy,
    secondary = LightGold,
    onSecondary = DarkNavy,
    tertiary = LightNavy,
    onTertiary = Gold,
    background = Color.White,
    onBackground = DarkNavy,
    surface = Color.White,
    onSurface = DarkNavy,
    surfaceVariant = LightNavy.copy(alpha = 0.1f),
    onSurfaceVariant = DarkNavy,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}