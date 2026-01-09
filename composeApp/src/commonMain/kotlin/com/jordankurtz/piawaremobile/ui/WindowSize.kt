package com.jordankurtz.piawaremobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classes following Material Design guidelines.
 * - Compact: Phone in portrait (< 600dp)
 * - Medium: Small tablet or phone in landscape (600-840dp)
 * - Expanded: Large tablet or desktop (> 840dp)
 */
enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded
}

data class WindowSize(
    val width: Dp,
    val height: Dp
) {
    val widthSizeClass: WindowSizeClass
        get() = when {
            width < 600.dp -> WindowSizeClass.Compact
            width < 840.dp -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }

    val isTablet: Boolean
        get() = widthSizeClass != WindowSizeClass.Compact
}

val LocalWindowSize = compositionLocalOf { WindowSize(0.dp, 0.dp) }
