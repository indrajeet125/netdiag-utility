package com.netdiag.tool.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.netdiag.tool.data.repository.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyanDark,
    onPrimary = CanvasNavyDark,
    primaryContainer = AccentCyanBgDark,
    onPrimaryContainer = AccentCyanDark,
    secondary = TextSecondaryDark,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SurfaceElevatedDark,
    onSecondaryContainer = TextPrimaryDark,
    background = CanvasNavyDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceBaseDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceBaseDark,
    surfaceContainerHigh = SurfaceElevatedDark,
    surfaceContainerHighest = SurfaceHighlightDark,
    outline = BorderSubtleDark,
    outlineVariant = BorderSubtleDark,
    error = StatusRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCyanLight,
    onPrimary = Color.White,
    primaryContainer = AccentCyanBgLight,
    onPrimaryContainer = AccentCyanLight,
    secondary = TextSecondaryLight,
    onSecondary = TextPrimaryLight,
    secondaryContainer = SurfaceElevatedLight,
    onSecondaryContainer = TextPrimaryLight,
    background = CanvasNeutralLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceBaseLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = SurfaceBaseLight,
    surfaceContainerHigh = SurfaceElevatedLight,
    surfaceContainerHighest = SurfaceHighlightLight,
    outline = BorderSubtleLight,
    outlineVariant = BorderSubtleLight,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

val NetDiagTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun NetDiagTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM_DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM_DEFAULT -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = colorScheme.background.toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NetDiagTypography,
        content = content
    )
}
