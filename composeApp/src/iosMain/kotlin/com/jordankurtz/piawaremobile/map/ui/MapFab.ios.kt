package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import maplibre.MapFabBlurView
import platform.UIKit.UIView

@Composable
actual fun MapFab(
    onClick: () -> Unit,
    modifier: Modifier,
    active: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        @OptIn(ExperimentalForeignApi::class)
        UIKitView<UIView>(
            factory = { MapFabBlurView.create() as UIView },
            modifier = Modifier.fillMaxSize(),
            properties =
                UIKitInteropProperties(
                    isInteractive = false,
                    isNativeAccessibilityEnabled = false,
                ),
        )
        if (active) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        )
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
