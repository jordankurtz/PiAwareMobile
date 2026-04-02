package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.offline_maps_picker_cancel
import piawaremobile.composeapp.generated.resources.offline_maps_picker_confirm
import kotlin.math.abs

private const val INITIAL_BOX_FRACTION = 0.6f
private val MIN_SIDE_LENGTH_DP = 80.dp
private val HANDLE_RADIUS_DP = 12.dp
private val HANDLE_HIT_TARGET_DP = 24.dp

@Composable
fun MapRegionPickerScreen(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapViewModel: MapViewModel = koinViewModel(),
) {
    MapRegionPickerContent(
        onRegionSelected = onRegionSelected,
        onDismiss = onDismiss,
        mapLayer = {
            OpenStreetMap(
                state = mapViewModel.state,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
internal fun MapRegionPickerContent(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapLayer: @Composable () -> Unit = {},
) {
    var boxLeft by remember { mutableFloatStateOf(0f) }
    var boxTop by remember { mutableFloatStateOf(0f) }
    var boxRight by remember { mutableFloatStateOf(0f) }
    var boxBottom by remember { mutableFloatStateOf(0f) }

    var initialized by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        mapLayer()

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        val w = size.toSize().width
                        val h = size.toSize().height
                        if (!initialized && w > 0f && h > 0f) {
                            val margin = (1f - INITIAL_BOX_FRACTION) / 2f
                            boxLeft = w * margin
                            boxTop = h * margin
                            boxRight = w * (1f - margin)
                            boxBottom = h * (1f - margin)
                            initialized = true
                        }
                    }
                    .pointerInput(Unit) {
                        val minSidePx = MIN_SIDE_LENGTH_DP.toPx()
                        val hitPx = HANDLE_HIT_TARGET_DP.toPx()

                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val x = change.position.x - dragAmount.x
                            val y = change.position.y - dragAmount.y
                            val dx = dragAmount.x
                            val dy = dragAmount.y

                            val midX = (boxLeft + boxRight) / 2f
                            val midY = (boxTop + boxBottom) / 2f

                            val handlePositions =
                                listOf(
                                    Offset(boxLeft, boxTop) to HandleType.CORNER_TL,
                                    Offset(boxRight, boxTop) to HandleType.CORNER_TR,
                                    Offset(boxLeft, boxBottom) to HandleType.CORNER_BL,
                                    Offset(boxRight, boxBottom) to HandleType.CORNER_BR,
                                    Offset(midX, boxTop) to HandleType.EDGE_TOP,
                                    Offset(midX, boxBottom) to HandleType.EDGE_BOTTOM,
                                    Offset(boxLeft, midY) to HandleType.EDGE_LEFT,
                                    Offset(boxRight, midY) to HandleType.EDGE_RIGHT,
                                )

                            val hit =
                                handlePositions.firstOrNull { (pos, _) ->
                                    abs(x - pos.x) < hitPx && abs(y - pos.y) < hitPx
                                }

                            if (hit != null) {
                                when (hit.second) {
                                    HandleType.CORNER_TL -> {
                                        boxLeft = (boxLeft + dx).coerceAtMost(boxRight - minSidePx)
                                        boxTop = (boxTop + dy).coerceAtMost(boxBottom - minSidePx)
                                    }
                                    HandleType.CORNER_TR -> {
                                        boxRight = (boxRight + dx).coerceAtLeast(boxLeft + minSidePx)
                                        boxTop = (boxTop + dy).coerceAtMost(boxBottom - minSidePx)
                                    }
                                    HandleType.CORNER_BL -> {
                                        boxLeft = (boxLeft + dx).coerceAtMost(boxRight - minSidePx)
                                        boxBottom = (boxBottom + dy).coerceAtLeast(boxTop + minSidePx)
                                    }
                                    HandleType.CORNER_BR -> {
                                        boxRight = (boxRight + dx).coerceAtLeast(boxLeft + minSidePx)
                                        boxBottom = (boxBottom + dy).coerceAtLeast(boxTop + minSidePx)
                                    }
                                    HandleType.EDGE_TOP -> {
                                        boxTop = (boxTop + dy).coerceAtMost(boxBottom - minSidePx)
                                    }
                                    HandleType.EDGE_BOTTOM -> {
                                        boxBottom = (boxBottom + dy).coerceAtLeast(boxTop + minSidePx)
                                    }
                                    HandleType.EDGE_LEFT -> {
                                        boxLeft = (boxLeft + dx).coerceAtMost(boxRight - minSidePx)
                                    }
                                    HandleType.EDGE_RIGHT -> {
                                        boxRight = (boxRight + dx).coerceAtLeast(boxLeft + minSidePx)
                                    }
                                }
                            }
                        }
                    },
        ) {
            if (!initialized) return@Canvas

            val handleRadiusPx = HANDLE_RADIUS_DP.toPx()
            val overlayColor = Color.Black.copy(alpha = 0.35f)
            val strokeColor = Color.White
            val handleFillColor = Color.White
            val strokeWidth = 2.dp.toPx()

            // Semi-transparent overlay outside the selection rectangle
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, boxTop),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, boxBottom),
                size = Size(size.width, size.height - boxBottom),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, boxTop),
                size = Size(boxLeft, boxBottom - boxTop),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(boxRight, boxTop),
                size = Size(size.width - boxRight, boxBottom - boxTop),
            )

            // Selection rectangle border
            drawRect(
                color = strokeColor,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxRight - boxLeft, boxBottom - boxTop),
                style = Stroke(width = strokeWidth),
            )

            val midX = (boxLeft + boxRight) / 2f
            val midY = (boxTop + boxBottom) / 2f

            // Corner handles
            val corners =
                listOf(
                    Offset(boxLeft, boxTop),
                    Offset(boxRight, boxTop),
                    Offset(boxLeft, boxBottom),
                    Offset(boxRight, boxBottom),
                )
            corners.forEach { center ->
                drawCircle(color = handleFillColor, radius = handleRadiusPx, center = center)
                drawCircle(
                    color = strokeColor,
                    radius = handleRadiusPx,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
            }

            // Edge midpoint handles
            val edgeMids =
                listOf(
                    Offset(midX, boxTop),
                    Offset(midX, boxBottom),
                    Offset(boxLeft, midY),
                    Offset(boxRight, midY),
                )
            edgeMids.forEach { center ->
                drawCircle(color = handleFillColor, radius = handleRadiusPx, center = center)
                drawCircle(
                    color = strokeColor,
                    radius = handleRadiusPx,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
            }
        }

        BottomAppBar(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.offline_maps_picker_cancel))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        // Coordinate conversion is deferred to the backend phase — return mock bounds
                        onRegionSelected(
                            BoundingBox(
                                minLat = 37.0,
                                maxLat = 38.0,
                                minLon = -122.5,
                                maxLon = -121.5,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.offline_maps_picker_confirm))
                }
            }
        }
    }
}

private enum class HandleType {
    CORNER_TL,
    CORNER_TR,
    CORNER_BL,
    CORNER_BR,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
}
