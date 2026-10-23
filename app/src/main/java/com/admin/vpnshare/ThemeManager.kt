package com.admin.vpnshare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class AccentColor(val displayName: String, val color: Color) {
    INDIGO("Indigo", Color(0xFF4F46E5)),
    EMERALD("Emerald", Color(0xFF10B981)),
    BLUE("Blue", Color(0xFF3B82F6)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    CRIMSON("Crimson", Color(0xFFE11D48)),
    AMBER("Amber", Color(0xFFF59E0B)),
    TEAL("Teal", Color(0xFF14B8A6)),
    ORANGE("Orange", Color(0xFFF97316))
}

object ThemeManager {
    var isDarkMode by mutableStateOf(true)

    var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var currentAccentColor by mutableStateOf(AccentColor.INDIGO)
        private set

    fun init(sharedPrefs: android.content.SharedPreferences, isSystemDark: Boolean = true) {
        val savedMode = sharedPrefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        currentThemeMode = try { ThemeMode.valueOf(savedMode) } catch (e: Exception) { ThemeMode.SYSTEM }

        isDarkMode = when (currentThemeMode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }

        val savedAccent = sharedPrefs.getString("accent_color", AccentColor.INDIGO.name) ?: AccentColor.INDIGO.name
        currentAccentColor = try { AccentColor.valueOf(savedAccent) } catch (e: Exception) { AccentColor.INDIGO }
    }

    fun syncWithSystem(isSystemDark: Boolean) {
        if (currentThemeMode == ThemeMode.SYSTEM) {
            isDarkMode = isSystemDark
        }
    }

    fun toggleTheme(context: android.content.Context) {
        // Toggle explicitly between DARK and LIGHT
        isDarkMode = !isDarkMode
        currentThemeMode = if (isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
        try {
            val prefs = context.getSharedPreferences("vpn_share_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_dark_mode", isDarkMode)
                .putString("theme_mode", currentThemeMode.name)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setThemeMode(mode: ThemeMode, sharedPrefs: android.content.SharedPreferences, isSystemDark: Boolean = true) {
        currentThemeMode = mode
        isDarkMode = when (mode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        sharedPrefs.edit()
            .putString("theme_mode", mode.name)
            .putBoolean("is_dark_mode", isDarkMode)
            .apply()
    }

    fun setAccentColor(accent: AccentColor, sharedPrefs: android.content.SharedPreferences) {
        currentAccentColor = accent
        sharedPrefs.edit().putString("accent_color", accent.name).apply()
    }
}
