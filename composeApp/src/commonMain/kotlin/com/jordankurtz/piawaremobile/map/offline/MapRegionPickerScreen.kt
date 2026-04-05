package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMap
import com.jordankurtz.piawaremobile.map.invertProjection
import com.jordankurtz.piawaremobile.map.mapSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_edit
import piawaremobile.composeapp.generated.resources.ic_map
import piawaremobile.composeapp.generated.resources.offline_maps_picker_cancel
import piawaremobile.composeapp.generated.resources.offline_maps_picker_confirm
import piawaremobile.composeapp.generated.resources.offline_maps_picker_mode_box
import piawaremobile.composeapp.generated.resources.offline_maps_picker_mode_map
import piawaremobile.composeapp.generated.resources.offline_maps_picker_switch_to_box_mode
import piawaremobile.composeapp.generated.resources.offline_maps_picker_switch_to_map_mode
import kotlin.math.abs

private const val INITIAL_BOX_FRACTION = 0.6f
private val MIN_SIDE_LENGTH_DP = 80.dp
private val HANDLE_RADIUS_DP = 12.dp
private val HANDLE_HIT_TARGET_DP = 24.dp

private enum class InteractionMode {
    MAP,
    BOX,
}

@Composable
fun MapRegionPickerScreen(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapViewModel: MapViewModel = koinViewModel(),
) {
    val scrollX = mapViewModel.state.scroll.x
    val scrollY = mapViewModel.state.scroll.y
    val scale = mapViewModel.state.scale
    MapRegionPickerContent(
        onRegionSelected = onRegionSelected,
        onDismiss = onDismiss,
        mapLayer = {
            OpenStreetMap(
                state = mapViewModel.state,
                modifier = Modifier.fillMaxSize(),
            )
        },
        scrollX = scrollX,
        scrollY = scrollY,
        scale = scale,
    )
}

@Composable
internal fun MapRegionPickerContent(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapLayer: @Composable () -> Unit,
    scrollX: Double = 0.0,
    scrollY: Double = 0.0,
    scale: Double = 1.0,
) {
    var bounds by remember { mutableStateOf(BoxBounds(0f, 0f, 0f, 0f)) }
    var initialized by remember { mutableStateOf(false) }
    var interactionMode by remember { mutableStateOf(InteractionMode.BOX) }
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        mapLayer()

        // The pointerInput modifier is only applied in BOX mode. In MAP mode the Canvas carries no
        // pointer handling at all, so all touch events fall through to the underlying map.
        val boxModePointerInput =
            if (interactionMode == InteractionMode.BOX) {
                Modifier.pointerInput(interactionMode) {
                    handleBoxGestures(
                        getBounds = { bounds },
                        setBounds = { bounds = it },
                        getScreenSize = { screenWidth to screenHeight },
                    )
                }
            } else {
                Modifier
            }

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        val w = size.toSize().width
                        val h = size.toSize().height
                        screenWidth = w
                        screenHeight = h
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
                    .then(boxModePointerInput),
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

        // Mode toggle FAB — top-end corner
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text =
                    stringResource(
                        if (interactionMode == InteractionMode.MAP) {
                            Res.string.offline_maps_picker_mode_map
                        } else {
                            Res.string.offline_maps_picker_mode_box
                        },
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            FloatingActionButton(
                onClick = {
                    interactionMode =
                        if (interactionMode == InteractionMode.MAP) {
                            InteractionMode.BOX
                        } else {
                            InteractionMode.MAP
                        }
                },
            ) {
                if (interactionMode == InteractionMode.MAP) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = stringResource(Res.string.offline_maps_picker_switch_to_box_mode),
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_map),
                        contentDescription = stringResource(Res.string.offline_maps_picker_switch_to_map_mode),
                    )
                }
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
                        val b = bounds
                        // state.scroll gives the top-left corner in scaled pixels.
                        // normX = (scrollX + screenX) / (mapSize * scale)
                        val scaledMapSize = mapSize * scale
                        val (topLat, leftLon) = invertProjection(
                            normX = (scrollX + b.left) / scaledMapSize,
                            normY = (scrollY + b.top) / scaledMapSize,
                        )
                        val (bottomLat, rightLon) = invertProjection(
                            normX = (scrollX + b.right) / scaledMapSize,
                            normY = (scrollY + b.bottom) / scaledMapSize,
                        )
                        onRegionSelected(
                            BoundingBox(
                                minLat = bottomLat,
                                maxLat = topLat,
                                minLon = leftLon,
                                maxLon = rightLon,
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

/**
 * Drives the pointer gesture loop for box-editing mode.
 *
 * Handles are checked first; if the down event lands inside the box (but not on a handle), the
 * entire box is translated on subsequent drag events. Events that miss both handles and the box
 * interior are not consumed, preserving fall-through for the underlying map.
 */
private suspend fun PointerInputScope.handleBoxGestures(
    getBounds: () -> BoxBounds,
    setBounds: (BoxBounds) -> Unit,
    getScreenSize: () -> Pair<Float, Float>,
) {
    val minSidePx = MIN_SIDE_LENGTH_DP.toPx()
    val hitPx = HANDLE_HIT_TARGET_DP.toPx()

    var activeHandle: HandleType? = null
    var isMovingBox = false
    var lastPosition: Offset = Offset.Zero

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            val position = change.position
            val bounds = getBounds()

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
                // Pointer down — check handle hit, then box interior, then ignore.
                change.pressed && !change.previousPressed -> {
                    val hit =
                        handlePositions.firstOrNull { (pos, _) ->
                            abs(position.x - pos.x) < hitPx && abs(position.y - pos.y) < hitPx
                        }
                    when {
                        hit != null -> {
                            activeHandle = hit.second
                            lastPosition = position
                            event.changes.forEach { it.consume() }
                        }
                        isInsideBox(position, bounds) -> {
                            isMovingBox = true
                            lastPosition = position
                            event.changes.forEach { it.consume() }
                        }
                        // Outside box and not on a handle — don't consume.
                    }
                }

                // Pointer move — apply handle drag or box translate.
                change.pressed && (activeHandle != null || isMovingBox) -> {
                    val dx = position.x - lastPosition.x
                    val dy = position.y - lastPosition.y
                    lastPosition = position
                    val (sw, sh) = getScreenSize()
                    setBounds(
                        if (activeHandle != null) {
                            applyHandleDrag(activeHandle!!, bounds, dx, dy, minSidePx)
                        } else {
                            bounds.translate(dx, dy, sw, sh)
                        },
                    )
                    event.changes.forEach { it.consume() }
                }

                // Pointer up — release active gesture.
                !change.pressed && change.previousPressed -> {
                    if (activeHandle != null || isMovingBox) {
                        event.changes.forEach { it.consume() }
                        activeHandle = null
                        isMovingBox = false
                    }
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

internal data class BoxBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal fun isInsideBox(
    position: Offset,
    bounds: BoxBounds,
): Boolean =
    position.x > bounds.left &&
        position.x < bounds.right &&
        position.y > bounds.top &&
        position.y < bounds.bottom

internal fun BoxBounds.translate(
    dx: Float,
    dy: Float,
    screenWidth: Float,
    screenHeight: Float,
): BoxBounds {
    val boxWidth = right - left
    val boxHeight = bottom - top
    val newLeft = (left + dx).coerceIn(0f, screenWidth - boxWidth)
    val newTop = (top + dy).coerceIn(0f, screenHeight - boxHeight)
    return BoxBounds(
        left = newLeft,
        top = newTop,
        right = newLeft + boxWidth,
        bottom = newTop + boxHeight,
    )
}

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
