package com.jordankurtz.piawaremobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Aviation-specific semantic color tokens that extend Material 3's color scheme.
 *
 * Access via [AppTheme.colors] inside any composable.
 *
 * These cover domain concepts (aircraft status, altitude bands, map overlays) that
 * have no equivalent in M3's generic token set.
 */
@Immutable
data class AppColors(
    // Aircraft status — drawn from standard aviation conventions:
    // green = airborne, amber = on-ground, red = emergency, purple = military.
    val aircraftAirborne: Color,
    val aircraftGround: Color,
    val aircraftEmergency: Color,
    val aircraftMilitary: Color,
    val aircraftSelected: Color,
    val aircraftUnknown: Color,
    // Altitude bands
    val altitudeHigh: Color,
    val altitudeLow: Color,
    // Map overlays
    val radarTint: Color,
    val trailHigh: Color,
    val trailLow: Color,
    // UI chrome
    val surfaceContainer: Color,
    val divider: Color,
    val positive: Color,
    val caution: Color,
)

// ---------------------------------------------------------------------------
// Default instances
// ---------------------------------------------------------------------------

internal val darkAppColors =
    AppColors(
        aircraftAirborne = Palette.Green500,
        aircraftGround = Palette.Amber500,
        aircraftEmergency = Palette.Red500,
        aircraftMilitary = Palette.Purple500,
        aircraftSelected = Palette.Sky400,
        aircraftUnknown = Palette.Gray500,
        altitudeHigh = Palette.Blue400,
        altitudeLow = Palette.Green400,
        radarTint = Palette.Green500.copy(alpha = 0.15f),
        trailHigh = Palette.Blue400.copy(alpha = 0.6f),
        trailLow = Palette.Green400.copy(alpha = 0.6f),
        surfaceContainer = Palette.Slate700,
        divider = Palette.Slate600,
        positive = Palette.Green500,
        caution = Palette.Amber500,
    )

// Darker variants of status colors to meet 4.5:1 contrast on light backgrounds.
private val LightAircraftAirborne = Color(0xFF15803D)
private val LightAircraftGround = Color(0xFFB45309)
private val LightAircraftMilitary = Color(0xFF7E22CE)
private val LightAircraftSelected = Color(0xFF0284C7)
private val LightAircraftUnknown = Color(0xFF4B5563)
private val LightAltitudeLow = Color(0xFF16A34A)

internal val lightAppColors =
    AppColors(
        aircraftAirborne = LightAircraftAirborne,
        aircraftGround = LightAircraftGround,
        aircraftEmergency = Palette.Red500,
        aircraftMilitary = LightAircraftMilitary,
        aircraftSelected = LightAircraftSelected,
        aircraftUnknown = LightAircraftUnknown,
        altitudeHigh = Palette.Blue600,
        altitudeLow = LightAltitudeLow,
        radarTint = Palette.Green500.copy(alpha = 0.10f),
        trailHigh = Palette.Blue600.copy(alpha = 0.5f),
        trailLow = LightAltitudeLow.copy(alpha = 0.5f),
        surfaceContainer = Color(0xFFEFF6FF),
        divider = Palette.LightOutline,
        positive = LightAircraftAirborne,
        caution = LightAircraftGround,
    )

// ---------------------------------------------------------------------------
// CompositionLocal + accessor
// ---------------------------------------------------------------------------

internal val LocalAppColors = staticCompositionLocalOf { darkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
