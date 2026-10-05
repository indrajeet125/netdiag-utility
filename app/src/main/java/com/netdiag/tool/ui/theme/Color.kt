package com.netdiag.tool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── Dark Palette (Engineering Slate / Deep Navy) ──
val CanvasNavyDark = Color(0xFF0C1017)
val SurfaceBaseDark = Color(0xFF141A23)
val SurfaceElevatedDark = Color(0xFF1C2430)
val SurfaceHighlightDark = Color(0xFF242E3D)
val BorderSubtleDark = Color(0xFF283446)
val TextPrimaryDark = Color(0xFFF0F4F8)
val TextSecondaryDark = Color(0xFF8E9BAE)
val TextTertiaryDark = Color(0xFF5E6B7E)
val AccentCyanDark = Color(0xFF00E5FF)
val AccentCyanBgDark = Color(0x1A00E5FF)

// ── Light Palette (Engineering Daylight Clean) ──
val CanvasNeutralLight = Color(0xFFF7F9FC)
val SurfaceBaseLight = Color(0xFFFFFFFF)
val SurfaceElevatedLight = Color(0xFFEFF3F8)
val SurfaceHighlightLight = Color(0xFFE2E8F0)
val BorderSubtleLight = Color(0xFFD3DCE6)
val TextPrimaryLight = Color(0xFF111827)
val TextSecondaryLight = Color(0xFF4B5563)
val TextTertiaryLight = Color(0xFF9CA3AF)
val AccentCyanLight = Color(0xFF00838F) // Deep readable teal for light mode
val AccentCyanBgLight = Color(0x1F00838F)

// ── Diagnostic Semantic Status Colors ──
val StatusGreen = Color(0xFF00E676)
val StatusGreenBg = Color(0x1A00E676)
val StatusAmber = Color(0xFFFFB300)
val StatusAmberBg = Color(0x1AFFB300)
val StatusRed = Color(0xFFFF5252)
val StatusRedBg = Color(0x1AFF5252)
val StatusBlue = Color(0xFF2979FF)
val StatusBlueBg = Color(0x1A2979FF)

// Dynamic theme-aware aliases (Adapt automatically to Light, Dark & System Theme)
val CanvasNavy: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val SurfaceBase: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val SurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val SurfaceHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHighest

val BorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val AccentCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val AccentCyanBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primaryContainer

val AccentCyanBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

val StatusGreenBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = StatusGreen.copy(alpha = 0.35f)

val StatusAmberBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = StatusAmber.copy(alpha = 0.35f)

val StatusRedBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = StatusRed.copy(alpha = 0.35f)

val StatusBlueBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = StatusBlue.copy(alpha = 0.35f)
