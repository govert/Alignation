package com.alignation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = StatusGreen,
    onSecondary = OnPrimaryLight,
    secondaryContainer = StatusGreenLight,
    onSecondaryContainer = StatusGreen,
    tertiary = StatusYellow,
    onTertiary = TextPrimary,
    tertiaryContainer = StatusYellowLight,
    onTertiaryContainer = TextPrimary,
    error = StatusRed,
    onError = OnPrimaryLight,
    errorContainer = StatusRedLight,
    onErrorContainer = StatusRed,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = StatusGreen,
    onSecondary = OnPrimaryDark,
    secondaryContainer = StatusGreen,
    onSecondaryContainer = StatusGreenLight,
    tertiary = StatusYellow,
    onTertiary = TextPrimary,
    tertiaryContainer = StatusYellow,
    onTertiaryContainer = TextPrimary,
    error = StatusRedLight,
    onError = StatusRed,
    errorContainer = StatusRed,
    onErrorContainer = StatusRedLight,
    background = BackgroundDark,
    onBackground = OnPrimaryDark,
    surface = SurfaceDark,
    onSurface = OnPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AlignationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
