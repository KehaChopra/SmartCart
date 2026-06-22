package com.yourbusiness.smartkart.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SmartKartLightColorScheme = lightColorScheme(
    primary = SmartKartGreen,
    onPrimary = Color.White,
    primaryContainer = SmartKartGreenLight,
    onPrimaryContainer = SmartKartGreenDark,
    background = SmartKartBackground,
    onBackground = SmartKartNavy,
    surface = SmartKartBackground,
    onSurface = SmartKartNavy,
    onSurfaceVariant = SmartKartTextSecondary,
    outline = SmartKartBorder,
    surfaceVariant = SmartKartButtonDisabled,
    error = SmartKartError,
    onError = Color.White,
    errorContainer = SmartKartDeleteRed,
    onErrorContainer = SmartKartDeleteIcon
)

private val SmartKartDarkColorScheme = darkColorScheme(
    primary = SmartKartGreen,
    onPrimary = Color.White,
    primaryContainer = SmartKartDarkGreenLight,
    onPrimaryContainer = SmartKartGreen,
    background = SmartKartDarkBackground,
    onBackground = SmartKartDarkOnSurface,
    surface = SmartKartDarkSurface,
    onSurface = SmartKartDarkOnSurface,
    onSurfaceVariant = SmartKartDarkTextSecondary,
    outline = SmartKartDarkBorder,
    surfaceVariant = SmartKartDarkButtonDisabled,
    error = SmartKartError,
    onError = Color.White,
    errorContainer = SmartKartDarkDeleteRed,
    onErrorContainer = SmartKartDeleteIcon
)

@Composable
fun SmartKartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> SmartKartDarkColorScheme
        else -> SmartKartLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
