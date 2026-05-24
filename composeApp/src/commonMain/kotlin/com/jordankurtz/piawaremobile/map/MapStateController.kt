package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow

interface MapStateController {
    val cameraFlow: Flow<MapPosition>

    var zoom: Double

    fun setZoomLimits(min: Double, max: Double)

    suspend fun setCamera(latitude: Double, longitude: Double, zoom: Double)

    suspend fun scrollTo(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    )

    suspend fun scrollTo(
        bounds: MapBounds,
        padding: Offset,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    )

    fun visibleBounds(): MapBounds

    fun screenToLatLon(screenX: Float, screenY: Float): LatLon

    fun onMarkerClick(handler: (id: String) -> Unit)

    fun onTap(handler: () -> Unit)

    fun onTouchDown(handler: () -> Unit)

    fun setSelectedMarker(id: String?)

    fun addMarker(
        id: String,
        latitude: Double,
        longitude: Double,
        content: @Composable () -> Unit,
    )

    fun removeMarker(id: String)

    fun addPath(
        id: String,
        color: Color,
        width: Dp,
        points: List<LatLon>,
    )

    fun removePath(id: String)
}
