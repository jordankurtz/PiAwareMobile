package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
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
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.onMarkerClick
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
    private var showAircraftPaths: Boolean = true
    private val previousAircraftMarkerIds = mutableSetOf<String>()
    private val previousPathIds = mutableSetOf<String>()

    private val _selectedAircraft = MutableStateFlow<String?>(null)
    val selectedAircraft: StateFlow<String?> = _selectedAircraft

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 16) {
        minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
    }.apply {
        addLayer(mapProvider)

        onMarkerClick { id, _, _ ->
            if (previousAircraftMarkerIds.contains(id)) {
                if (_selectedAircraft.value == id) {
                    _selectedAircraft.value = null
                } else {
                    _selectedAircraft.value = id
                }
            }
        }
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

    private suspend fun onSettingsLoaded(settings: Settings) {
        showAircraftPaths = settings.showAircraftPaths
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
        saveStateJob = viewModelScope.launch {
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

    fun onReceiverLocation(receiver: Map.Entry<Server, Location>) = viewModelScope.launch {
        val (x, y) = receiver.value.projected
        state.addMarker(
            receiver.key.id.toString(), x, y
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_receiver),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }

    fun recenterOnLocation(location: Location) {
        viewModelScope.launch {
            val (x, y) = location.projected
            Logger.d("Scrolling map to $x, $y")
            state.scrollTo(x, y)
        }
    }

    fun onUserLocationChanged(location: Location) {
        val (x, y) = location.projected
        state.removeMarker(USER_LOCATION_MARKER_ID)
        state.addMarker(
            id = USER_LOCATION_MARKER_ID,
            x = x,
            y = y
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_user_location),
                contentDescription = stringResource(Res.string.user_location_content_description),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    fun onAircraftUpdated(aircraft: List<Pair<Aircraft, Any?>>) {
        previousAircraftMarkerIds.forEach(state::removeMarker)
        previousAircraftMarkerIds.clear()

        aircraft.forEach { (plane, _) ->
            val location = doProjection(plane.lat, plane.lon)

            state.addMarker(
                plane.hex.also { previousAircraftMarkerIds.add(it) },
                location.first,
                location.second
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_plane),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(plane.track ?: 0f),
                    colorFilter = ColorFilter.tint(getColorForAltitude(plane.altBaro))
                )
            }
        }
    }

    fun onAircraftTrailsUpdated(trails: Map<String, AircraftTrail>) {
        if (!showAircraftPaths) {
            clearPaths()
            return
        }

        previousPathIds.forEach { state.removePath(it) }
        previousPathIds.clear()

        trails.forEach { (hex, trail) ->
            if (trail.positions.size >= 2) {
                // Group consecutive positions by altitude color to reduce path count
                val colorSegments = groupPositionsByAltitudeColor(trail.positions)

                colorSegments.forEachIndexed { index, segment ->
                    if (segment.positions.size >= 2) {
                        val id = "trail_${hex}_$index"
                        previousPathIds.add(id)

                        val projectedPoints = segment.positions.map { pos ->
                            doProjection(pos.latitude, pos.longitude)
                        }

                        state.addPath(id, color = segment.color) {
                            addPoints(projectedPoints)
                        }
                    }
                }
            }
        }
    }

    private data class ColorSegment(
        val color: androidx.compose.ui.graphics.Color,
        val positions: MutableList<AircraftPosition>
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
