package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder

data class MapScrollPosition(val scrollX: Double, val scrollY: Double, val scale: Double)

@Suppress("TooManyFunctions")
interface MapStateController {
    val scrollAndScaleFlow: Flow<MapScrollPosition>

    fun addLayer(provider: TileStreamProvider): String

    fun onMarkerClick(handler: (id: String) -> Unit)

    fun onTap(handler: () -> Unit)

    fun onTouchDown(handler: () -> Unit)

    fun replaceLayer(
        layerId: String,
        provider: TileStreamProvider,
    ): String?

    suspend fun setScroll(
        x: Double,
        y: Double,
    )

    var scale: Double

    fun setScaleLimits(
        minScale: Double,
        maxScale: Double,
    )

    suspend fun scrollTo(
        x: Double,
        y: Double,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    )

    suspend fun scrollTo(
        area: BoundingBox,
        padding: Offset,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    )

    fun addMarker(
        id: String,
        x: Double,
        y: Double,
        relativeOffset: Offset,
        content: @Composable () -> Unit,
    )

    fun removeMarker(id: String)

    fun addPath(
        id: String,
        color: Color,
        width: Dp,
        init: PathDataBuilder.() -> Unit,
    )

    fun removePath(id: String)
}
