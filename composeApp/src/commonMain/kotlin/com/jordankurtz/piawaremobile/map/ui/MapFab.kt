package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    content: @Composable () -> Unit,
)
