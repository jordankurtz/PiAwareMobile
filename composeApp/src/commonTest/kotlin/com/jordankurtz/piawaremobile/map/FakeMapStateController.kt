package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder

class FakeMapStateController : MapStateController {
    override val scrollAndScaleFlow: Flow<MapScrollPosition> = emptyFlow()

    override fun addLayer(provider: TileStreamProvider): String = "fake-layer-id"

    override fun onMarkerClick(handler: (id: String) -> Unit) = Unit

    override fun onTap(handler: () -> Unit) = Unit

    override fun onTouchDown(handler: () -> Unit) = Unit

    override fun replaceLayer(
        layerId: String,
        provider: TileStreamProvider,
    ): String? = null

    override suspend fun setScroll(
        x: Double,
        y: Double,
    ) = Unit

    override var scale: Double = 1.0

    var lastMinScale: Double = 0.0
    var lastMaxScale: Double = Double.MAX_VALUE

    override fun setScaleLimits(minScale: Double, maxScale: Double) {
        lastMinScale = minScale
        lastMaxScale = maxScale
    }

    override suspend fun scrollTo(
        x: Double,
        y: Double,
        animationSpec: AnimationSpec<Float>,
    ) = Unit

    override suspend fun scrollTo(
        area: BoundingBox,
        padding: Offset,
        animationSpec: AnimationSpec<Float>,
    ) = Unit

    override fun addMarker(
        id: String,
        x: Double,
        y: Double,
        relativeOffset: Offset,
        content: @Composable () -> Unit,
    ) = Unit

    override fun removeMarker(id: String) = Unit

    override fun addPath(
        id: String,
        color: Color,
        width: Dp,
        init: PathDataBuilder.() -> Unit,
    ) = Unit

    override fun removePath(id: String) = Unit
}
