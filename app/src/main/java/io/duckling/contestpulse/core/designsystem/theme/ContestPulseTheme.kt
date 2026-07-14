package io.duckling.contestpulse.core.designsystem.theme

import android.animation.ValueAnimator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors are not provided")
}

private val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography is not provided")
}

private val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
private val LocalAppRadius = staticCompositionLocalOf { AppRadius() }
private val LocalAppElevation = staticCompositionLocalOf { AppElevation() }
private val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun ContestPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides DefaultAppTypography,
        LocalAppSpacing provides AppSpacing(),
        LocalAppRadius provides AppRadius(),
        LocalAppElevation provides AppElevation(),
        LocalMotionEnabled provides ValueAnimator.areAnimatorsEnabled(),
        content = content,
    )
}

object PulseTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current

    val typography: AppTypography
        @Composable get() = LocalAppTypography.current

    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current

    val radius: AppRadius
        @Composable get() = LocalAppRadius.current

    val elevation: AppElevation
        @Composable get() = LocalAppElevation.current

    val motionEnabled: Boolean
        @Composable get() = LocalMotionEnabled.current
}
