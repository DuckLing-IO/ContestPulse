package io.duckling.contestpulse.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val onBackground: Color,
    val onSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val separator: Color,
)

internal val LightAppColors = AppColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF6E6E73),
    textTertiary = Color(0xFFAEAEB2),
    accent = Color(0xFF000000),
    onAccent = Color(0xFFFFFFFF),
    separator = Color(0xFFE5E5EA),
)

internal val DarkAppColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceMuted = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366),
    accent = Color(0xFFF5F5F5),
    onAccent = Color(0xFF000000),
    separator = Color(0xFF38383A),
)
