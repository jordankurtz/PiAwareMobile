package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMapStateController : MapStateController {
    val cameraStateFlow = MutableStateFlow(MapPosition(0.0, 0.0, 5.0))
    override val cameraFlow: Flow<MapPosition> = cameraStateFlow

    override var zoom: Double = 5.0

    var lastMinZoom: Double = 0.0
    var lastMaxZoom: Double = 22.0

    override fun setZoomLimits(
        min: Double,
        max: Double,
    ) {
        lastMinZoom = min
        lastMaxZoom = max
    }

    val setCameraArgs = mutableListOf<Triple<Double, Double, Double>>()

    override suspend fun setCamera(
        latitude: Double,
        longitude: Double,
        zoom: Double,
    ) {
        setCameraArgs.add(Triple(latitude, longitude, zoom))
        cameraStateFlow.value = MapPosition(latitude, longitude, zoom)
        this.zoom = zoom
    }

    val scrolledToPositions = mutableListOf<MapPosition>()
    val scrolledToBounds = mutableListOf<MapBounds>()

    override suspend fun scrollTo(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        animationSpec: AnimationSpec<Float>,
    ) {
        scrolledToPositions.add(MapPosition(latitude, longitude, zoom))
    }

    override suspend fun scrollTo(
        bounds: MapBounds,
        padding: Offset,
        animationSpec: AnimationSpec<Float>,
    ) {
        scrolledToBounds.add(bounds)
    }

    override fun visibleBounds(): MapBounds = MapBounds(90.0, -90.0, 180.0, -180.0)

    override fun screenToLatLon(
        screenX: Float,
        screenY: Float,
    ): LatLon = LatLon(0.0, 0.0)

    var markerClickHandler: ((String) -> Unit)? = null

    override fun onMarkerClick(handler: (id: String) -> Unit) {
        markerClickHandler = handler
    }

    var tapHandler: (() -> Unit)? = null

    override fun onTap(handler: () -> Unit) {
        tapHandler = handler
    }

    var touchDownHandler: (() -> Unit)? = null

    override fun onTouchDown(handler: () -> Unit) {
        touchDownHandler = handler
    }

    var selectedMarkerId: String? = null

    override fun setSelectedMarker(id: String?) {
        selectedMarkerId = id
    }

    val addedMarkers = mutableMapOf<String, Pair<Double, Double>>()

    override fun addMarker(
        id: String,
        latitude: Double,
        longitude: Double,
        content: @Composable () -> Unit,
    ) {
        addedMarkers[id] = latitude to longitude
    }

    val removedMarkerIds = mutableListOf<String>()

    override fun removeMarker(id: String) {
        addedMarkers.remove(id)
        removedMarkerIds.add(id)
    }

    val addedPaths = mutableMapOf<String, List<LatLon>>()

    override fun addPath(
        id: String,
        color: Color,
        width: Dp,
        points: List<LatLon>,
    ) {
        addedPaths[id] = points
    }

    val removedPathIds = mutableListOf<String>()

    override fun removePath(id: String) {
        addedPaths.remove(id)
        removedPathIds.add(id)
    }
}
