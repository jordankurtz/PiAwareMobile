package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.reset_north
import kotlin.math.abs
import kotlin.math.min

@Composable
fun CompassFab(
    bearing: Float,
    onResetNorth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = remember(bearing) { min(1f, abs(bearing) / 10f + 0.25f) }
    val label = stringResource(Res.string.reset_north)
    MapFab(
        onClick = onResetNorth,
        modifier =
            modifier
                .graphicsLayer { this.alpha = alpha }
                .semantics { contentDescription = label },
    ) {
        val northColor = Color(0xFFEF4444)
        val southColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        Canvas(
            modifier =
                Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = -bearing },
        ) {
            val halfW = size.width * 0.22f
            val midY = size.height / 2f
            val cx = size.width / 2f

            drawPath(
                path =
                    Path().apply {
                        moveTo(cx, 0f)
                        lineTo(cx + halfW, midY)
                        lineTo(cx - halfW, midY)
                        close()
                    },
                color = northColor,
            )
            drawPath(
                path =
                    Path().apply {
                        moveTo(cx, size.height)
                        lineTo(cx + halfW, midY)
                        lineTo(cx - halfW, midY)
                        close()
                    },
                color = southColor,
            )
        }
    }
}
