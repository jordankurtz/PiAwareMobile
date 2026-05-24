package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.concurrent.Volatile
import kotlin.math.cos
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

/**
 * Holds Compose-observable marker and path collections plus a reference to a
 * maplibre-compose [CameraState]. The [MapLibreMap] composable owns the actual
 * [CameraState] instance and binds it to this controller via [setCameraState] for
 * the duration of the composition.
 *
 * Markers are exposed as a [mutableStateMapOf] so the composable can render them
 * reactively. maplibre-compose 0.12.1 does NOT support Compose view annotations,
 * so the actual rendering strategy for marker content is handled at the call
 * site (see Task 11 for the planned symbol-layer based approach). The controller
 * itself just stores the data.
 *
 * Paths are rendered by the composable as GeoJSON line layers.
 */
class MapLibreStateController : MapStateController {

    data class MarkerData(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val content: @Composable () -> Unit,
    )

    data class PathData(
        val id: String,
        val color: Color,
        val width: Dp,
        val points: List<LatLon>,
    )

    val markers = mutableStateMapOf<String, MarkerData>()
    val paths = mutableStateMapOf<String, PathData>()

    private val cameraFlowState = MutableStateFlow(MapPosition(0.0, 0.0, 0.0))
    override val cameraFlow: StateFlow<MapPosition> = cameraFlowState.asStateFlow()

    @Volatile
    private var cameraState: CameraState? = null

    private var pixelDensity: Float = 1f

    private var _minZoom by mutableStateOf(0.0)
    private var _maxZoom by mutableStateOf(22.0)

    var selectedMarkerId: String? by mutableStateOf(null)
        private set

    private var markerClickHandler: ((String) -> Unit)? = null
    private var tapHandler: (() -> Unit)? = null
    private var touchDownHandler: (() -> Unit)? = null

    fun setDensity(density: Density) {
        pixelDensity = density.density
    }

    /**
     * Called by [MapLibreMap] when it enters composition. Pass `null` from
     * `onDispose` to break the reference and avoid leaking the composable's
     * camera state past the composable lifecycle.
     */
    fun setCameraState(state: CameraState?) {
        cameraState = state
        state?.position?.let { position ->
            cameraFlowState.value = position.toMapPosition()
        }
    }

    /**
     * Called by [MapLibreMap] whenever the maplibre-compose camera state
     * changes. Emits a new [MapPosition] on [cameraFlow].
     */
    fun onCameraChanged(latitude: Double, longitude: Double, zoom: Double) {
        cameraFlowState.value = MapPosition(latitude, longitude, zoom)
    }

    override var zoom: Double
        get() = cameraState?.position?.zoom ?: cameraFlowState.value.zoom
        set(value) {
            val state = cameraState ?: return
            val current = state.position
            state.position = current.copy(zoom = value)
        }

    override fun setZoomLimits(min: Double, max: Double) {
        _minZoom = min
        _maxZoom = max
        // Limits applied to the live MapAdapter inside the composable; here we
        // simply record them so the composable can pick them up.
    }

    fun zoomLimits(): ClosedFloatingPointRange<Float> =
        _minZoom.toFloat().._maxZoom.toFloat()

    override suspend fun setCamera(latitude: Double, longitude: Double, zoom: Double) {
        val state = cameraState ?: run {
            cameraFlowState.value = MapPosition(latitude, longitude, zoom)
            return
        }
        state.position = state.position.copy(
            target = Position(longitude = longitude, latitude = latitude),
            zoom = zoom,
        )
    }

    override suspend fun scrollTo(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        animationSpec: AnimationSpec<Float>, // animationSpec is not used; maplibre-compose animates with a fixed duration
    ) {
        val state = cameraState ?: run {
            cameraFlowState.value = MapPosition(latitude, longitude, zoom)
            return
        }
        val target = state.position.copy(
            target = Position(longitude = longitude, latitude = latitude),
            zoom = zoom,
        )
        state.animateTo(target, duration = DEFAULT_ANIMATION_DURATION_MS.milliseconds)
    }

    override suspend fun scrollTo(
        bounds: MapBounds,
        padding: Offset,
        animationSpec: AnimationSpec<Float>, // animationSpec is not used; maplibre-compose animates with a fixed duration
    ) {
        val state = cameraState ?: return
        val bbox = bounds.toBoundingBox()
        val paddingValues = PaddingValues(
            horizontal = padding.x.dp,
            vertical = padding.y.dp,
        )
        state.animateTo(
            boundingBox = bbox,
            bearing = state.position.bearing,
            tilt = state.position.tilt,
            padding = paddingValues,
            duration = DEFAULT_ANIMATION_DURATION_MS.milliseconds,
        )
    }

    override fun visibleBounds(): MapBounds {
        val state = cameraState ?: return approximateBounds()
        // The projection is null until the map has rendered a frame; querying
        // it may also throw if maplibre's internals aren't ready yet. Fall
        // back to an approximation in either case.
        val bbox =
            try {
                state.projection?.queryVisibleBoundingBox()
            } catch (_: Throwable) {
                null
            } ?: return approximateBounds()
        return MapBounds(
            north = bbox.north,
            south = bbox.south,
            east = bbox.east,
            west = bbox.west,
        )
    }

    /**
     * Approximate visible bounds from the current camera position when the
     * maplibre projection isn't available yet (e.g. before the map has rendered
     * a frame). Uses the simple "world is a square at zoom 0" approximation:
     * one tile spans 360 degrees longitude at zoom 0.
     */
    private fun approximateBounds(): MapBounds {
        val position = cameraFlowState.value
        val degreesPerTile = 360.0 / 2.0.pow(position.zoom)
        val latSpan = degreesPerTile * cos(position.latitude * kotlin.math.PI / 180.0)
        return MapBounds(
            north = position.latitude + latSpan / 2.0,
            south = position.latitude - latSpan / 2.0,
            east = position.longitude + degreesPerTile / 2.0,
            west = position.longitude - degreesPerTile / 2.0,
        )
    }

    override fun screenToLatLon(screenX: Float, screenY: Float): LatLon {
        val d = pixelDensity
        val dpOffset = DpOffset(x = (screenX / d).dp, y = (screenY / d).dp)
        val position = cameraState?.projection?.positionFromScreenLocation(dpOffset)
            ?: return LatLon(0.0, 0.0)
        return LatLon(latitude = position.latitude, longitude = position.longitude)
    }

    override fun onMarkerClick(handler: (id: String) -> Unit) {
        markerClickHandler = handler
    }

    override fun onTap(handler: () -> Unit) {
        tapHandler = handler
    }

    override fun onTouchDown(handler: () -> Unit) {
        touchDownHandler = handler
    }

    fun handleMapTap() {
        tapHandler?.invoke()
    }

    fun handleMarkerTap(id: String) {
        markerClickHandler?.invoke(id)
    }

    fun handleTouchDown() {
        touchDownHandler?.invoke()
    }

    override fun setSelectedMarker(id: String?) {
        selectedMarkerId = id
    }

    override fun addMarker(
        id: String,
        latitude: Double,
        longitude: Double,
        content: @Composable () -> Unit,
    ) {
        markers[id] = MarkerData(id, latitude, longitude, content)
    }

    override fun removeMarker(id: String) {
        markers.remove(id)
    }

    override fun addPath(
        id: String,
        color: Color,
        width: Dp,
        points: List<LatLon>,
    ) {
        paths[id] = PathData(id, color, width, points)
    }

    override fun removePath(id: String) {
        paths.remove(id)
    }

    companion object {
        const val DEFAULT_ANIMATION_DURATION_MS: Long = 500L
    }
}

private fun CameraPosition.toMapPosition(): MapPosition =
    MapPosition(
        latitude = target.latitude,
        longitude = target.longitude,
        zoom = zoom,
    )

private fun MapBounds.toBoundingBox(): org.maplibre.spatialk.geojson.BoundingBox =
    org.maplibre.spatialk.geojson.BoundingBox(
        west = west,
        south = south,
        east = east,
        north = north,
    )
