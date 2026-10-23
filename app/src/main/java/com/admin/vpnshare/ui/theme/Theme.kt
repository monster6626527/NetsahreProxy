package com.admin.vpnshare.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.admin.vpnshare.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF07070F),
    secondary = NeonPurple,
    onSecondary = Color.White,
    tertiary = NeonGreen,
    background = Color(0xFF07070F),
    onBackground = Color.White,
    surface = Color(0xFF0E0E1C),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141428),
    onSurfaceVariant = Color(0xFF8892A4),
    error = NeonRed,
    onError = Color.White,
    outline = Color(0x2600E5FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    tertiary = Color(0xFF059669),
    background = Color(0xFFF3F6FA),
    onBackground = Color(0xFF0B132B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B132B),
    surfaceVariant = Color(0xFFEBF1F7),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0x26000000)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()

    // Sync with system when system theme changes
    LaunchedEffect(systemInDark) {
        ThemeManager.syncWithSystem(systemInDark)
    }

    val isDark = ThemeManager.isDarkMode
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When in light theme, status bar & nav bar icons must be DARK so they are clearly visible
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

