package com.jordankurtz.piawaremobile.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Raw palette — named values only, no semantic meaning at this layer.
// Prefer referencing AppColors or MaterialTheme.colorScheme in UI code.
// ---------------------------------------------------------------------------

internal object Palette {
    // Neutrals — dark navy scale
    val Slate950 = Color(0xFF020917)
    val Slate900 = Color(0xFF0D1526)
    val Slate850 = Color(0xFF111D30)
    val Slate800 = Color(0xFF152035)
    val Slate700 = Color(0xFF1C2A40)
    val Slate600 = Color(0xFF1E293B)
    val Slate500 = Color(0xFF334155)
    val Slate400 = Color(0xFF475569)
    val Slate300 = Color(0xFF64748B)
    val Slate200 = Color(0xFF94A3B8)
    val Slate100 = Color(0xFFCBD5E1)
    val Slate50 = Color(0xFFE2E8F0)
    val White = Color(0xFFF8FAFC)

    // Blues — instrument / radar
    val Blue700 = Color(0xFF1D4ED8)
    val Blue600 = Color(0xFF2563EB)
    val Blue500 = Color(0xFF3B82F6)
    val Blue400 = Color(0xFF60A5FA)
    val Blue300 = Color(0xFF93C5FD)

    // Sky — horizon / altitude highlights
    val Sky500 = Color(0xFF0EA5E9)
    val Sky400 = Color(0xFF38BDF8)
    val Sky300 = Color(0xFF7DD3FC)

    // Status colors — drawn from standard aviation conventions
    val Green500 = Color(0xFF22C55E) // airborne / normal
    val Green400 = Color(0xFF4ADE80)
    val Amber500 = Color(0xFFF59E0B) // on-ground / caution
    val Amber400 = Color(0xFFFBBF24)
    val Red500 = Color(0xFFEF4444) // emergency / alert
    val Red400 = Color(0xFFF87171)
    val Purple500 = Color(0xFFA855F7) // military
    val Purple400 = Color(0xFFC084FC)
    val Gray500 = Color(0xFF6B7280) // unknown / filtered

    // Light mode neutrals
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFEFF6FF)
    val LightOnSurface = Color(0xFF0F172A)
    val LightOnSurfaceVariant = Color(0xFF334155)
    val LightOutline = Color(0xFFCBD5E1)
    val LightOutlineVariant = Color(0xFFE2E8F0)
}

// ---------------------------------------------------------------------------
// Material 3 color schemes
// ---------------------------------------------------------------------------

internal val DarkColorScheme =
    darkColorScheme(
        primary = Palette.Blue500,
        onPrimary = Palette.White,
        primaryContainer = Palette.Slate800,
        onPrimaryContainer = Palette.Blue300,
        secondary = Palette.Sky500,
        onSecondary = Palette.Slate950,
        secondaryContainer = Palette.Slate700,
        onSecondaryContainer = Palette.Sky300,
        tertiary = Palette.Sky400,
        onTertiary = Palette.Slate950,
        tertiaryContainer = Palette.Slate800,
        onTertiaryContainer = Palette.Sky300,
        error = Palette.Red500,
        onError = Palette.White,
        errorContainer = Color(0xFF7F1D1D),
        onErrorContainer = Palette.Red400,
        background = Palette.Slate950,
        onBackground = Palette.Slate50,
        surface = Palette.Slate900,
        onSurface = Palette.Slate50,
        surfaceVariant = Palette.Slate800,
        onSurfaceVariant = Palette.Slate200,
        outline = Palette.Slate500,
        outlineVariant = Palette.Slate600,
        scrim = Color(0xCC000000),
        inverseSurface = Palette.Slate50,
        inverseOnSurface = Palette.Slate950,
        inversePrimary = Palette.Blue700,
    )

internal val LightColorScheme =
    lightColorScheme(
        primary = Palette.Blue600,
        onPrimary = Palette.White,
        primaryContainer = Palette.LightSurfaceVariant,
        onPrimaryContainer = Palette.Blue700,
        secondary = Palette.Sky500,
        onSecondary = Palette.White,
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = Color(0xFF0369A1),
        tertiary = Palette.Sky400,
        onTertiary = Palette.White,
        tertiaryContainer = Color(0xFFE0F2FE),
        onTertiaryContainer = Color(0xFF0284C7),
        error = Palette.Red500,
        onError = Palette.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF991B1B),
        background = Palette.LightBackground,
        onBackground = Palette.LightOnSurface,
        surface = Palette.LightSurface,
        onSurface = Palette.LightOnSurface,
        surfaceVariant = Palette.LightSurfaceVariant,
        onSurfaceVariant = Palette.LightOnSurfaceVariant,
        outline = Palette.LightOutline,
        outlineVariant = Palette.LightOutlineVariant,
        scrim = Color(0x99000000),
        inverseSurface = Palette.Slate900,
        inverseOnSurface = Palette.Slate50,
        inversePrimary = Palette.Blue400,
    )
