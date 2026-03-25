package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.annotation.Factory
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.onTouchDown
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScroll
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_receiver
import piawaremobile.composeapp.generated.resources.ic_user_location
import piawaremobile.composeapp.generated.resources.user_location_content_description
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

private const val USER_LOCATION_MARKER_ID = "user_location"

@Suppress("TooManyFunctions")
@OptIn(FlowPreview::class)
@Factory
class MapViewModel(
    private val mapProvider: TileStreamProvider,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
) : ViewModel() {
    private var saveStateJob: Job? = null
    private var settings: Settings? = null
    private val previousAircraftMarkerIds = mutableSetOf<String>()
    private val previousPathIds = mutableSetOf<String>()
    private var lastTrails: Map<String, AircraftTrail> = emptyMap()

    private val _selectedAircraft = MutableStateFlow<String?>(null)
    val selectedAircraft: StateFlow<String?> = _selectedAircraft

    private val _followingAircraft = MutableStateFlow<String?>(null)
    val followingAircraft: StateFlow<String?> = _followingAircraft

    private val _followingUserLocation = MutableStateFlow(false)
    val followingUserLocation: StateFlow<Boolean> = _followingUserLocation

    private val _showUserLocationOnMap = MutableStateFlow(false)
    val showUserLocationOnMap: StateFlow<Boolean> = _showUserLocationOnMap

    /** Exposes the last location passed to [recenterOnLocation] for test verification. */
    internal val lastRecenteredLocation = MutableStateFlow<Location?>(null)

    private val _trailSelectedAircraft = MutableStateFlow<String?>(null)

    val state =
        MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 16) {
            minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
        }.apply {
            addLayer(mapProvider)

            onMarkerClick { id, _, _ ->
                if (previousAircraftMarkerIds.contains(id)) {
                    val newSelection = if (_selectedAircraft.value == id) null else id
                    _selectedAircraft.value = newSelection
                    _trailSelectedAircraft.value = newSelection
                    onAircraftTrailsUpdated(lastTrails)
                }
            }

            onTap { _, _ ->
                // Deselect when tapping on empty space
                if (_selectedAircraft.value != null) {
                    _selectedAircraft.value = null
                    _followingAircraft.value = null
                    _trailSelectedAircraft.value = null
                    onAircraftTrailsUpdated(lastTrails)
                }
            }

            onTouchDown { onMapTouchDown() }
        }

    init {
        viewModelScope.launch {
            loadSettingsUseCase().collect {
                when (it) {
                    is Async.Success -> {
                        settings = it.data
                        onSettingsLoaded(it.data)
                    }
                    is Async.Error -> {
                        Logger.e("Failed to load settings", it.throwable)
                    }

                    else -> {
                        // No-op
                    }
                }
            }
        }
    }

    fun onAircraftDeselected() {
        _selectedAircraft.value = null
        _followingAircraft.value = null
        _trailSelectedAircraft.value = null
        onAircraftTrailsUpdated(lastTrails)
    }

    fun followSelectedAircraft() {
        _followingAircraft.value = _selectedAircraft.value
    }

    fun unfollowAircraft() {
        _followingAircraft.value = null
    }

    /**
     * Sync selection from external source (e.g., tablet list panel).
     * Used to keep map and list selections in sync.
     */
    fun syncSelection(hex: String?) {
        if (_selectedAircraft.value != hex) {
            _selectedAircraft.value = hex
            _trailSelectedAircraft.value = hex
            onAircraftTrailsUpdated(lastTrails)
        }
    }

    private suspend fun onSettingsLoaded(settings: Settings) {
        this.settings = settings
        _showUserLocationOnMap.value = settings.showUserLocationOnMap
        if (!settings.showUserLocationOnMap) {
            _followingUserLocation.value = false
        }
        onAircraftTrailsUpdated(lastTrails)
        saveStateJob?.cancel()
        if (settings.restoreMapStateOnStart) {
            loadMapState()
            startSaveMapStateJob()
        }
    }

    private suspend fun loadMapState() {
        val savedState = getSavedMapStateUseCase()
        Logger.d("Restored map state $savedState")
        state.setScroll(savedState.scrollX, savedState.scrollY)
        state.scale = savedState.zoom
    }

    private fun startSaveMapStateJob() {
        saveStateJob =
            viewModelScope.launch {
                snapshotFlow { Pair(state.scroll, state.scale) }
                    .debounce(500.milliseconds)
                    .onEach { (scroll, scale) ->
                        if (scroll.x > 0.0 && scroll.y > 0.0) {
                            saveMapStateUseCase(scroll.x, scroll.y, scale)
                            Logger.d("Saved map state $scroll, $scale")
                        }
                    }.launchIn(this)
            }
    }

    fun onReceiverLocation(receiver: Map.Entry<Server, Location>) =
        viewModelScope.launch {
            val (x, y) = receiver.value.projected
            state.addMarker(
                id = receiver.key.id.toString(),
                x = x,
                y = y,
                relativeOffset = Offset(-0.5f, -0.5f),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_receiver),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(20.dp),
                )
            }
        }

    fun recenterOnLocation(location: Location) {
        lastRecenteredLocation.value = location
        viewModelScope.launch {
            val (x, y) = location.projected
            Logger.d("Scrolling map to $x, $y")
            state.scrollTo(x, y)
        }
    }

    fun fitToAircraft(aircraft: List<AircraftWithServers>) {
        val coordinates = aircraft.map { it.aircraft.lat to it.aircraft.lon }
        when (val target = computeFitTarget(coordinates)) {
            null -> return
            is FitTarget.SinglePoint -> {
                viewModelScope.launch {
                    state.scrollTo(
                        target.x,
                        target.y,
                        animationSpec = SpringSpec(stiffness = Spring.StiffnessLow),
                    )
                }
            }
            is FitTarget.BoundingRegion -> {
                val boundingBox =
                    BoundingBox(
                        xLeft = target.xLeft,
                        yTop = target.yTop,
                        xRight = target.xRight,
                        yBottom = target.yBottom,
                    )
                viewModelScope.launch {
                    state.scrollTo(
                        area = boundingBox,
                        padding = Offset(x = 0.15f, y = 0.15f),
                        animationSpec = SpringSpec(stiffness = Spring.StiffnessLow),
                    )
                }
            }
        }
    }

    fun toggleFollowUserLocation() {
        _followingUserLocation.value = !_followingUserLocation.value
    }

    internal fun onMapTouchDown() {
        if (_followingUserLocation.value) {
            _followingUserLocation.value = false
        }
    }

    fun onUserLocationChanged(location: Location) {
        if (_followingUserLocation.value) {
            recenterOnLocation(location)
        }
        val (x, y) = location.projected
        state.removeMarker(USER_LOCATION_MARKER_ID)
        state.addMarker(
            id = USER_LOCATION_MARKER_ID,
            x = x,
            y = y,
            relativeOffset = Offset(-0.5f, -0.5f),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_user_location),
                contentDescription = stringResource(Res.string.user_location_content_description),
                modifier = Modifier.size(24.dp),
            )
        }
    }

    fun onAircraftUpdated(aircraft: List<AircraftWithServers>) {
        previousAircraftMarkerIds.forEach(state::removeMarker)
        previousAircraftMarkerIds.clear()

        var followedAircraftPosition: Pair<Double, Double>? = null
        val followingHex = _followingAircraft.value

        aircraft.forEach { item ->
            val plane = item.aircraft
            val location = doProjection(plane.lat, plane.lon)

            if (followingHex != null && plane.hex == followingHex) {
                followedAircraftPosition = location
            }

            state.addMarker(
                id = plane.hex.also { previousAircraftMarkerIds.add(it) },
                x = location.first,
                y = location.second,
                relativeOffset = Offset(-0.5f, -0.5f),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_plane),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(30.dp)
                            .rotate(plane.track ?: 0f),
                    colorFilter = ColorFilter.tint(getColorForAltitude(plane.altBaro)),
                )
            }
        }

        followedAircraftPosition?.let { (x, y) ->
            viewModelScope.launch {
                state.scrollTo(x, y)
            }
        }

        if (followingHex != null && followedAircraftPosition == null) {
            _followingAircraft.value = null
        }
    }

    fun onAircraftTrailsUpdated(trails: Map<String, AircraftTrail>) {
        lastTrails = trails
        clearPaths()

        val mode = settings?.trailDisplayMode ?: TrailDisplayMode.ALL

        val trailsToDisplay =
            when (mode) {
                TrailDisplayMode.NONE -> emptyMap()
                TrailDisplayMode.ALL -> trails
                TrailDisplayMode.SELECTED -> {
                    val selectedHex = _trailSelectedAircraft.value ?: _selectedAircraft.value
                    if (selectedHex != null) {
                        trails.filterKeys { it == selectedHex }
                    } else {
                        emptyMap()
                    }
                }
            }

        trailsToDisplay.forEach { (hex, trail) ->
            drawTrail(hex, trail)
        }
    }

    private fun drawTrail(
        hex: String,
        trail: AircraftTrail,
    ) {
        if (trail.positions.size >= 2) {
            // Group consecutive positions by altitude color to reduce path count
            val colorSegments = groupPositionsByAltitudeColor(trail.positions)

            colorSegments.forEachIndexed { index, segment ->
                if (segment.positions.size >= 2) {
                    val id = "trail_${hex}_$index"
                    previousPathIds.add(id)

                    val projectedPoints =
                        segment.positions.map { pos ->
                            doProjection(pos.latitude, pos.longitude)
                        }

                    state.addPath(
                        id = id,
                        color = segment.color,
                        width = 1.5.dp,
                    ) {
                        addPoints(projectedPoints)
                    }
                }
            }
        }
    }

    private data class ColorSegment(
        val color: androidx.compose.ui.graphics.Color,
        val positions: MutableList<AircraftPosition>,
    )

    private fun groupPositionsByAltitudeColor(positions: List<AircraftPosition>): List<ColorSegment> {
        if (positions.isEmpty()) return emptyList()

        val segments = mutableListOf<ColorSegment>()
        var currentColor = getColorForAltitude(positions.first().altitude)
        var currentSegment = ColorSegment(currentColor, mutableListOf(positions.first()))

        for (i in 1 until positions.size) {
            val pos = positions[i]
            val posColor = getColorForAltitude(pos.altitude)

            if (posColor == currentColor) {
                currentSegment.positions.add(pos)
            } else {
                // End current segment and start new one
                // Add last point of current segment as first point of new segment for continuity
                segments.add(currentSegment)
                currentColor = posColor
                currentSegment = ColorSegment(currentColor, mutableListOf(positions[i - 1], pos))
            }
        }

        segments.add(currentSegment)
        return segments
    }

    private fun clearPaths() {
        previousPathIds.forEach { state.removePath(it) }
        previousPathIds.clear()
    }
}
