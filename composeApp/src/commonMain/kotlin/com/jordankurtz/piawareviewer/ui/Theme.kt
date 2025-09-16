package com.jordankurtz.piawareviewer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Checks if your system is in dark theme mode.
    content: @Composable () -> Unit
) {
    val colors = if (!darkTheme) LightColorScheme else LightColorScheme
    MaterialTheme(
        colors = colors,
        content = content,
    )
}