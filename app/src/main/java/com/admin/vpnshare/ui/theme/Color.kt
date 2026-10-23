package com.admin.vpnshare.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.admin.vpnshare.ThemeManager

// Dynamic Adaptive Design System Colors
val BgColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xFF07070F) else Color(0xFFF8FAFC)

val SurfaceColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xFF0E0E1C) else Color(0xFFFFFFFF)

val SurfaceVariantColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xFF141428) else Color(0xFFF1F5F9)

val BorderCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0x2600E5FF) else Color(0x330284C7)

val CardBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0x2600E5FF) else Color(0x140F172A)

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xFFFFFFFF) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xFF8892A4) else Color(0xFF475569)

val GlassBgDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xD90E1122) else Color(0xF5FFFFFF)

val GlassBgBase: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0xEB080A14) else Color(0xF0F8FAFC)

val GlassBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) Color(0x5500E5FF) else Color(0x250284C7)

// Adaptive High-Contrast Cyber Status Colors (Legible in both Dark & Light modes)
val StatusGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) NeonGreen else Color(0xFF059669)

val StatusCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) NeonCyan else Color(0xFF0284C7)

val StatusAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) NeonAmber else Color(0xFFD97706)

val StatusRed: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) NeonRed else Color(0xFFDC2626)

val StatusPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = if (ThemeManager.isDarkMode) NeonPurple else Color(0xFF7C3AED)

// High-Tech Cyber Accent Raw Colors
val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFF9D4EDD)
val NeonGreen = Color(0xFF00FF87)
val NeonRed = Color(0xFFFF2D55)
val NeonAmber = Color(0xFFFFB800)

