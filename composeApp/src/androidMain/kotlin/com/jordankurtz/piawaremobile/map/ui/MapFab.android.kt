package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun MapFab(
    onClick: () -> Unit,
    modifier: Modifier,
    active: Boolean,
    content: @Composable () -> Unit,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor =
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        content = content,
    )
}
