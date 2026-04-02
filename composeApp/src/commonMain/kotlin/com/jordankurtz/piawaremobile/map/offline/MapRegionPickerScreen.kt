package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
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
    mapLayer: @Composable () -> Unit,
) {
    var bounds by remember { mutableStateOf(BoxBounds(0f, 0f, 0f, 0f)) }
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
                            bounds =
                                BoxBounds(
                                    left = w * margin,
                                    top = h * margin,
                                    right = w * (1f - margin),
                                    bottom = h * (1f - margin),
                                )
                            initialized = true
                        }
                    }
                    .pointerInput(Unit) {
                        val minSidePx = MIN_SIDE_LENGTH_DP.toPx()
                        val hitPx = HANDLE_HIT_TARGET_DP.toPx()

                        // Track which handle is being dragged and the last pointer position so we
                        // can compute deltas across move events.
                        var activeHandle: HandleType? = null
                        var lastPosition: Offset = Offset.Zero

                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: continue
                                val position = change.position

                                val midX = (bounds.left + bounds.right) / 2f
                                val midY = (bounds.top + bounds.bottom) / 2f

                                val handlePositions =
                                    listOf(
                                        Offset(bounds.left, bounds.top) to HandleType.CORNER_TL,
                                        Offset(bounds.right, bounds.top) to HandleType.CORNER_TR,
                                        Offset(bounds.left, bounds.bottom) to HandleType.CORNER_BL,
                                        Offset(bounds.right, bounds.bottom) to HandleType.CORNER_BR,
                                        Offset(midX, bounds.top) to HandleType.EDGE_TOP,
                                        Offset(midX, bounds.bottom) to HandleType.EDGE_BOTTOM,
                                        Offset(bounds.left, midY) to HandleType.EDGE_LEFT,
                                        Offset(bounds.right, midY) to HandleType.EDGE_RIGHT,
                                    )

                                when {
                                    // Pointer down — check if it lands on a handle.
                                    change.pressed && !change.previousPressed -> {
                                        val hit =
                                            handlePositions.firstOrNull { (pos, _) ->
                                                abs(position.x - pos.x) < hitPx &&
                                                    abs(position.y - pos.y) < hitPx
                                            }
                                        if (hit != null) {
                                            activeHandle = hit.second
                                            lastPosition = position
                                            event.changes.forEach { it.consume() }
                                        }
                                        // No hit — don't consume; map receives the down event.
                                    }

                                    // Pointer move — only handle if we grabbed a handle on down.
                                    change.pressed && activeHandle != null -> {
                                        val dx = position.x - lastPosition.x
                                        val dy = position.y - lastPosition.y
                                        lastPosition = position
                                        bounds = applyHandleDrag(activeHandle!!, bounds, dx, dy, minSidePx)
                                        event.changes.forEach { it.consume() }
                                    }

                                    // Pointer up — release the active handle.
                                    !change.pressed && change.previousPressed -> {
                                        if (activeHandle != null) {
                                            event.changes.forEach { it.consume() }
                                            activeHandle = null
                                        }
                                        // No active handle — don't consume; map receives the up event.
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

            val b = bounds

            // Semi-transparent overlay outside the selection rectangle
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, b.top),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, b.bottom),
                size = Size(size.width, size.height - b.bottom),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(0f, b.top),
                size = Size(b.left, b.bottom - b.top),
            )
            drawRect(
                color = overlayColor,
                topLeft = Offset(b.right, b.top),
                size = Size(size.width - b.right, b.bottom - b.top),
            )

            // Selection rectangle border
            drawRect(
                color = strokeColor,
                topLeft = Offset(b.left, b.top),
                size = Size(b.right - b.left, b.bottom - b.top),
                style = Stroke(width = strokeWidth),
            )

            val midX = (b.left + b.right) / 2f
            val midY = (b.top + b.bottom) / 2f

            // Corner handles
            val corners =
                listOf(
                    Offset(b.left, b.top),
                    Offset(b.right, b.top),
                    Offset(b.left, b.bottom),
                    Offset(b.right, b.bottom),
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
                    Offset(midX, b.top),
                    Offset(midX, b.bottom),
                    Offset(b.left, midY),
                    Offset(b.right, midY),
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

private data class BoxBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun applyHandleDrag(
    handle: HandleType,
    bounds: BoxBounds,
    dx: Float,
    dy: Float,
    minSidePx: Float,
): BoxBounds =
    when (handle) {
        HandleType.CORNER_TL ->
            bounds.copy(
                left = (bounds.left + dx).coerceAtMost(bounds.right - minSidePx),
                top = (bounds.top + dy).coerceAtMost(bounds.bottom - minSidePx),
            )
        HandleType.CORNER_TR ->
            bounds.copy(
                right = (bounds.right + dx).coerceAtLeast(bounds.left + minSidePx),
                top = (bounds.top + dy).coerceAtMost(bounds.bottom - minSidePx),
            )
        HandleType.CORNER_BL ->
            bounds.copy(
                left = (bounds.left + dx).coerceAtMost(bounds.right - minSidePx),
                bottom = (bounds.bottom + dy).coerceAtLeast(bounds.top + minSidePx),
            )
        HandleType.CORNER_BR ->
            bounds.copy(
                right = (bounds.right + dx).coerceAtLeast(bounds.left + minSidePx),
                bottom = (bounds.bottom + dy).coerceAtLeast(bounds.top + minSidePx),
            )
        HandleType.EDGE_TOP ->
            bounds.copy(top = (bounds.top + dy).coerceAtMost(bounds.bottom - minSidePx))
        HandleType.EDGE_BOTTOM ->
            bounds.copy(bottom = (bounds.bottom + dy).coerceAtLeast(bounds.top + minSidePx))
        HandleType.EDGE_LEFT ->
            bounds.copy(left = (bounds.left + dx).coerceAtMost(bounds.right - minSidePx))
        HandleType.EDGE_RIGHT ->
            bounds.copy(right = (bounds.right + dx).coerceAtLeast(bounds.left + minSidePx))
    }
