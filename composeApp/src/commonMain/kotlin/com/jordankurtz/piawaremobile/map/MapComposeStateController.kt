package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.onTouchDown
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.replaceLayer
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScroll
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

class MapComposeStateController : MapStateController {
    val mapState: MapState =
        MapState(
            levelCount = MAX_LEVEL + 1,
            mapSize,
            mapSize,
            workerCount = 16,
        ) {
            minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
        }

    override val scrollAndScaleFlow: Flow<MapScrollPosition> =
        snapshotFlow {
            val s = mapState.scroll
            MapScrollPosition(s.x, s.y, mapState.scale)
        }

    override fun addLayer(provider: TileStreamProvider): String = mapState.addLayer(provider)

    override fun onMarkerClick(handler: (id: String) -> Unit) {
        mapState.onMarkerClick { id, _, _ -> handler(id) }
    }

    override fun onTap(handler: () -> Unit) {
        mapState.onTap { _, _ -> handler() }
    }

    override fun onTouchDown(handler: () -> Unit) {
        mapState.onTouchDown { handler() }
    }

    override fun replaceLayer(
        layerId: String,
        provider: TileStreamProvider,
    ): String? = mapState.replaceLayer(layerId, provider)

    override suspend fun setScroll(
        x: Double,
        y: Double,
    ) = mapState.setScroll(x, y)

    override var scale: Double
        get() = mapState.scale
        set(value) {
            mapState.scale = value
        }

    override suspend fun scrollTo(
        x: Double,
        y: Double,
        animationSpec: AnimationSpec<Float>,
    ) = mapState.scrollTo(x, y, animationSpec = animationSpec)

    override suspend fun scrollTo(
        area: BoundingBox,
        padding: Offset,
        animationSpec: AnimationSpec<Float>,
    ) = mapState.scrollTo(area = area, padding = padding, animationSpec = animationSpec)

    override fun addMarker(
        id: String,
        x: Double,
        y: Double,
        relativeOffset: Offset,
        content: @Composable () -> Unit,
    ) = mapState.addMarker(id, x, y, relativeOffset = relativeOffset, c = content)

    override fun removeMarker(id: String) {
        mapState.removeMarker(id)
    }

    override fun addPath(
        id: String,
        color: Color,
        width: Dp,
        init: PathDataBuilder.() -> Unit,
    ) {
        mapState.addPath(id, color = color, width = width, builder = init)
    }

    override fun removePath(id: String) = mapState.removePath(id)

    fun shutdown() = mapState.shutdown()
}
