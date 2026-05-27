package com.raul1x9.mineconsole.views

import androidx.compose.ui.graphics.Color

data class ThemeColors(
    val background: Color,
    val cardBackground: Color,
    val text: Color,
    val subText: Color,
    val border: Color
)

object ThemeManager {
    fun getAccentColor(name: String): Color {
        return when (name) {
            "Blue" -> Color(0xFF0099FF)
            "Red" -> Color(0xFFFF3333)
            "Purple" -> Color(0xFFAE59FF)
            "Orange" -> Color(0xFFFF9900)
            else -> Color(0xFF00FF66) // Green (Default)
        }
    }

    fun getThemeColors(themeName: String, isSystemDark: Boolean): ThemeColors {
        val isDark = when (themeName) {
            "Light" -> false
            "Dark" -> true
            else -> isSystemDark // System (reads standard hardware dark mode)
        }

        return if (isDark) {
            ThemeColors(
                background = Color(0xFF0C0C0C),
                cardBackground = Color(0xFF1E1E1E),
                text = Color.White,
                subText = Color.White.copy(alpha = 0.5f),
                border = Color.White.copy(alpha = 0.05f)
            )
        } else {
            ThemeColors(
                background = Color(0xFFF5F5F5), // Premium off-white gray background
                cardBackground = Color.White,
                text = Color(0xFF1A1A1A),
                subText = Color(0xFF1A1A1A).copy(alpha = 0.6f),
                border = Color(0xFF1A1A1A).copy(alpha = 0.1f)
            )
        }
    }
}
