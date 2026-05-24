package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap as MaplibreComposeMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLibreMap(
    controller: MapLibreStateController,
    styleUrl: String,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
) {
    val cameraState = rememberCameraState()
    val density = LocalDensity.current

    DisposableEffect(controller, cameraState) {
        controller.setDensity(density)
        controller.setCameraState(cameraState)
        onDispose { controller.setCameraState(null) }
    }

    var cameraPosition by remember { mutableStateOf(cameraState.position) }
    LaunchedEffect(controller, cameraState) {
        snapshotFlow { cameraState.position }
            .collectLatest { position ->
                cameraPosition = position
                controller.onCameraChanged(
                    latitude = position.target.latitude,
                    longitude = position.target.longitude,
                    zoom = position.zoom,
                )
            }
    }

    val gestureOptions =
        if (gesturesEnabled) GestureOptions.Standard else GestureOptions.AllDisabled
    val mapOptions = remember(gestureOptions) {
        MapOptions(gestureOptions = gestureOptions)
    }

    val zoomRange = controller.zoomLimits()

    Box(modifier = modifier) {
        MaplibreComposeMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = cameraState,
            zoomRange = zoomRange,
            options = mapOptions,
            onMapClick = { _, _ ->
                controller.handleMapTap()
                ClickResult.Pass
            },
        ) {
            controller.paths.values.forEach { path -> PathLayer(path) }
        }

        val projection = cameraState.projection
        if (projection != null) {
            @Suppress("UNUSED_EXPRESSION") cameraPosition
            controller.markers.values.forEach { marker ->
                val screenPos = projection.screenLocationFromPosition(
                    Position(longitude = marker.longitude, latitude = marker.latitude)
                )
                Box(
                    modifier = Modifier
                        .size(0.dp)
                        .absoluteOffset { IntOffset(screenPos.x.roundToPx(), screenPos.y.roundToPx()) }
                        .wrapContentSize(unbounded = true),
                    contentAlignment = Alignment.Center,
                ) {
                    marker.content()
                }
            }
        }
    }
}

@Composable
private fun PathLayer(path: MapLibreStateController.PathData) {
    val positions = remember(path.points) {
        path.points.map { Position(longitude = it.longitude, latitude = it.latitude) }
    }
    val geoJson = remember(positions) {
        GeoJsonData.Features(LineString(positions))
    }
    val source = rememberGeoJsonSource(data = geoJson)
    LineLayer(
        id = "path-${path.id}",
        source = source,
        color = const(path.color),
        width = const(path.width),
    )
}
