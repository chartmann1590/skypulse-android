package com.charles.skypulse.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SkyDarkColorScheme = darkColorScheme(
    primary = SkyColors.PrimaryFixedDim,
    onPrimary = SkyColors.OnPrimary,
    primaryContainer = SkyColors.PrimaryContainer,
    onPrimaryContainer = SkyColors.OnPrimaryContainer,
    secondary = SkyColors.Secondary,
    onSecondary = SkyColors.OnSecondary,
    secondaryContainer = SkyColors.SecondaryContainer,
    onSecondaryContainer = SkyColors.OnSecondaryContainer,
    tertiary = SkyColors.TertiaryContainer,
    background = SkyColors.PitchBlack,
    onBackground = SkyColors.OnSurface,
    surface = SkyColors.Surface,
    onSurface = SkyColors.OnSurface,
    surfaceVariant = SkyColors.SurfaceVariant,
    onSurfaceVariant = SkyColors.OnSurfaceVariant,
    surfaceContainer = SkyColors.SurfaceContainer,
    surfaceContainerHigh = SkyColors.SurfaceContainerHigh,
    surfaceContainerHighest = SkyColors.SurfaceContainerHighest,
    surfaceContainerLow = SkyColors.SurfaceContainerLow,
    surfaceContainerLowest = SkyColors.SurfaceContainerLowest,
    outline = SkyColors.Outline,
    outlineVariant = SkyColors.OutlineVariant,
    error = SkyColors.Error,
    onError = SkyColors.OnError,
    errorContainer = SkyColors.ErrorContainer,
    onErrorContainer = SkyColors.OnErrorContainer,
)

@Composable
fun SkyPulseTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // SkyPulse is always dark by design.
    val colorScheme = SkyDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SkyTypography,
        shapes = SkyShapes,
        content = content,
    )
}
