package com.sleeplogger.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SleepDarkColorScheme = darkColorScheme(
    primary = MutedPurple,
    secondary = SoftBlue,
    tertiary = SubtleYellow,
    background = Charcoal,
    surface = DeepGray,
    onPrimary = Charcoal,
    onSecondary = Charcoal,
    onTertiary = Charcoal,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SoftGray,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SleepLoggerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SleepDarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
