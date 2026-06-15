package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.sources.rememberVectorSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.maplibre.compose.map.MaplibreMap as MaplibreComposeMap

// Tiles are only available from zoom 8 (minScale ~1:2.3M) to zoom 12 (maxScale ~1:144K)
private const val FAA_SECTIONAL_TILE_URL =
    "https://tiles.arcgis.com/tiles/ssFJjBXIUyZDrSYZ/arcgis/rest/services/VFR_Sectional/MapServer/tile/{z}/{y}/{x}"

// Correct ArcGIS service name is IFR_AreaLow, not IFR_Low
private const val FAA_IFR_LOW_TILE_URL =
    "https://tiles.arcgis.com/tiles/ssFJjBXIUyZDrSYZ/arcgis/rest/services/IFR_AreaLow/MapServer/tile/{z}/{y}/{x}"

private const val FAA_IFR_HIGH_TILE_URL =
    "https://tiles.arcgis.com/tiles/ssFJjBXIUyZDrSYZ/arcgis/rest/services/IFR_High/MapServer/tile/{z}/{y}/{x}"

private const val OPENAIP_TILE_URL_TEMPLATE =
    "https://api.tiles.openaip.net/api/data/openaip/{z}/{x}/{y}.pbf?apiKey="

@Suppress("LongParameterList", "UnusedParameter")
@Composable
fun MapLibreMap(
    controller: MapLibreStateController,
    styleUrl: String,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    onBearingChanged: (Float) -> Unit = {},
    faaChartsEnabled: Boolean = false,
    faaIfrLowEnabled: Boolean = false,
    faaIfrHighEnabled: Boolean = false,
    tfrsEnabled: Boolean = false,
    airspaceEnabled: Boolean = false,
    openAipApiKey: String = "",
    topStart: @Composable () -> Unit = {},
    topEnd: @Composable () -> Unit = {},
    bottomStart: @Composable () -> Unit = {},
    bottomEnd: @Composable () -> Unit = {},
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
                onBearingChanged(position.bearing.toFloat())
            }
    }

    val gestureOptions =
        if (gesturesEnabled) GestureOptions.Standard else GestureOptions.AllDisabled
    val mapOptions =
        remember(gestureOptions) {
            MapOptions(
                gestureOptions = gestureOptions,
                ornamentOptions = defaultOrnamentOptions(),
            )
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

            // Sources are always registered so the map style stays stable when toggling.
            // Visibility is controlled via opacity to avoid style reloads that reset the camera.
            // VFR Sectional tiles only exist at zoom 8–12; MapLibre skips requests outside this range
            val faaSource =
                rememberRasterSource(
                    tiles = listOf(FAA_SECTIONAL_TILE_URL),
                    options = TileSetOptions(minZoom = 8, maxZoom = 12),
                )
            RasterLayer(
                id = "faa-sectional",
                source = faaSource,
                opacity = if (faaChartsEnabled) const(0.6f) else const(0f),
            )

            val ifrLowSource = rememberRasterSource(tiles = listOf(FAA_IFR_LOW_TILE_URL), options = TileSetOptions())
            RasterLayer(
                id = "faa-ifr-low",
                source = ifrLowSource,
                opacity = if (faaIfrLowEnabled) const(0.6f) else const(0f),
            )

            val ifrHighSource = rememberRasterSource(tiles = listOf(FAA_IFR_HIGH_TILE_URL), options = TileSetOptions())
            RasterLayer(
                id = "faa-ifr-high",
                source = ifrHighSource,
                opacity = if (faaIfrHighEnabled) const(0.6f) else const(0f),
            )

            // TFR layer omitted: FAA does not expose a public GeoJSON endpoint

            val airspaceColor =
                switch(
                    feature.get("icaoClass").asNumber(),
                    case(0, const(Color(0xFF4169E1))),
                    case(1, const(Color(0xFF0047AB))),
                    case(2, const(Color(0xFF800080))),
                    case(3, const(Color(0xFF1E90FF))),
                    case(4, const(Color(0xFFDA70D6))),
                    case(5, const(Color(0xFF808080))),
                    fallback = const(Color(0xFFFF4444)),
                )
            val airspaceOpacity = if (airspaceEnabled && openAipApiKey.isNotEmpty()) 1f else 0f
            if (openAipApiKey.isNotEmpty()) {
                val airspaceSource =
                    rememberVectorSource(
                        tiles = listOf(OPENAIP_TILE_URL_TEMPLATE + openAipApiKey),
                        options = TileSetOptions(minZoom = 7, maxZoom = 14),
                    )
                FillLayer(
                    id = "openaip-airspace-fill",
                    source = airspaceSource,
                    sourceLayer = "openaip",
                    color = airspaceColor,
                    opacity = const(0.15f * airspaceOpacity),
                )
                LineLayer(
                    id = "openaip-airspace-border",
                    source = airspaceSource,
                    sourceLayer = "openaip",
                    color = airspaceColor,
                    width = const(1.5.dp),
                    opacity = const(0.8f * airspaceOpacity),
                )
            }
        }

        val projection = cameraState.projection
        if (projection != null) {
            @Suppress("UNUSED_EXPRESSION")
            cameraPosition
            controller.markers.values.forEach { marker ->
                val screenPos =
                    projection.screenLocationFromPosition(
                        Position(longitude = marker.longitude, latitude = marker.latitude),
                    )
                Box(
                    modifier =
                        Modifier
                            .size(0.dp)
                            .absoluteOffset { IntOffset(screenPos.x.roundToPx(), screenPos.y.roundToPx()) }
                            .wrapContentSize(unbounded = true)
                            .clickable { controller.handleMarkerTap(marker.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    val isSelected = marker.id == controller.selectedMarkerId
                    if (isSelected) {
                        Box(
                            modifier =
                                Modifier
                                    .size(38.dp)
                                    .border(2.dp, Color.White, CircleShape),
                        )
                    }
                    marker.content()
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopStart)) { topStart() }
        Box(modifier = Modifier.align(Alignment.TopEnd)) { topEnd() }
        Box(modifier = Modifier.align(Alignment.BottomStart)) { bottomStart() }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) { bottomEnd() }
    }
}

@Composable
private fun PathLayer(path: MapLibreStateController.PathData) {
    val positions =
        remember(path.points) {
            path.points.map { Position(longitude = it.longitude, latitude = it.latitude) }
        }
    val geoJson =
        remember(positions) {
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
