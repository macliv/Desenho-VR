package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    secondary = EmeraldLock,
    onSecondary = EmeraldOnLock,
    tertiary = AmberTorch,
    onTertiary = AmberOnTorch,
    error = CrimsonLost,
    onError = StudioTextPrimary,
    background = StudioBgVoid,
    onBackground = StudioTextPrimary,
    surface = StudioCardSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioGlassPanel,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder
)

@Composable
fun CameraLucidaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
