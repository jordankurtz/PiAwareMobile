package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.map.debug.TileCacheStats
import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.annotation.Factory
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_receiver
import piawaremobile.composeapp.generated.resources.ic_user_location
import piawaremobile.composeapp.generated.resources.user_location_content_description
import kotlin.time.Duration.Companion.milliseconds

private const val USER_LOCATION_MARKER_ID = "user_location"

@Suppress("TooManyFunctions")
@OptIn(FlowPreview::class)
@Factory
class MapViewModel(
    private val providerConfigFlow: StateFlow<TileProviderConfig>,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val tileCacheStatsTracker: TileCacheStatsTracker,
    internal val mapStateController: MapStateController,
) : ViewModel() {
    val activeProvider: StateFlow<TileProviderConfig> = providerConfigFlow

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

    val tileStats: StateFlow<TileCacheStats> = tileCacheStatsTracker.stats

    val currentZoomLevel: StateFlow<Int> =
        mapStateController.cameraFlow
            .map { it.zoom.toInt().coerceAtLeast(1) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 5)

    private val _zoomSettings = MutableStateFlow<Triple<Int, Int, Int>?>(null)
    val zoomSettings: StateFlow<Triple<Int, Int, Int>?> = _zoomSettings

    internal val lastRecenteredLocation = MutableStateFlow<Location?>(null)

    private val _trailSelectedAircraft = MutableStateFlow<String?>(null)

    init {
        mapStateController.onMarkerClick { id ->
            if (previousAircraftMarkerIds.contains(id)) {
                val newSelection = if (_selectedAircraft.value == id) null else id
                _selectedAircraft.value = newSelection
                _trailSelectedAircraft.value = newSelection
                mapStateController.setSelectedMarker(newSelection)
                onAircraftTrailsUpdated(lastTrails)
            }
        }

        mapStateController.onTap {
            if (_selectedAircraft.value != null) {
                _selectedAircraft.value = null
                _followingAircraft.value = null
                _trailSelectedAircraft.value = null
                mapStateController.setSelectedMarker(null)
                onAircraftTrailsUpdated(lastTrails)
            }
        }

        mapStateController.onTouchDown { onMapTouchDown() }

        viewModelScope.launch {
            loadSettingsUseCase().collect {
                when (it) {
                    is Async.Success -> {
                        settings = it.data
                        onSettingsLoaded(it.data)
                    }
                    is Async.Error -> Logger.e("Failed to load settings", it.throwable)
                    else -> Unit
                }
            }
        }
    }

    fun onAircraftDeselected() {
        _selectedAircraft.value = null
        _followingAircraft.value = null
        _trailSelectedAircraft.value = null
        mapStateController.setSelectedMarker(null)
        onAircraftTrailsUpdated(lastTrails)
    }

    fun followSelectedAircraft() {
        _followingAircraft.value = _selectedAircraft.value
    }

    fun unfollowAircraft() {
        _followingAircraft.value = null
    }

    fun syncSelection(hex: String?) {
        if (_selectedAircraft.value != hex) {
            _selectedAircraft.value = hex
            _trailSelectedAircraft.value = hex
            mapStateController.setSelectedMarker(hex)
            onAircraftTrailsUpdated(lastTrails)
        }
    }

    private suspend fun onSettingsLoaded(settings: Settings) {
        this.settings = settings
        _zoomSettings.value = Triple(settings.minZoomLevel, settings.maxZoomLevel, settings.defaultZoomLevel)
        _showUserLocationOnMap.value = settings.showUserLocationOnMap
        if (!settings.showUserLocationOnMap) _followingUserLocation.value = false
        onAircraftTrailsUpdated(lastTrails)
        mapStateController.setZoomLimits(
            settings.minZoomLevel.toDouble(),
            settings.maxZoomLevel.toDouble(),
        )
        saveStateJob?.cancel()
        if (settings.restoreMapStateOnStart) {
            loadMapState(settings.minZoomLevel, settings.maxZoomLevel)
            startSaveMapStateJob()
        } else {
            mapStateController.zoom = settings.defaultZoomLevel.toDouble()
        }
    }

    private suspend fun loadMapState(
        minZoom: Int,
        maxZoom: Int,
    ) {
        val savedState = getSavedMapStateUseCase()
        mapStateController.setCamera(
            latitude = savedState.latitude,
            longitude = savedState.longitude,
            zoom = savedState.zoom.coerceIn(minZoom.toDouble(), maxZoom.toDouble()),
        )
    }

    private fun startSaveMapStateJob() {
        saveStateJob =
            mapStateController.cameraFlow
                .debounce(500.milliseconds)
                .onEach { position ->
                    saveMapStateUseCase(
                        latitude = position.latitude,
                        longitude = position.longitude,
                        zoom = position.zoom,
                    )
                }
                .launchIn(viewModelScope)
    }

    fun onReceiverLocation(receiver: Map.Entry<Server, Location>) =
        viewModelScope.launch {
            mapStateController.addMarker(
                id = receiver.key.id.toString(),
                latitude = receiver.value.latitude,
                longitude = receiver.value.longitude,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_receiver),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

    fun recenterOnLocation(location: Location) {
        lastRecenteredLocation.value = location
        viewModelScope.launch {
            Logger.d("Scrolling map to ${location.latitude}, ${location.longitude}")
            mapStateController.scrollTo(location.latitude, location.longitude, mapStateController.zoom)
        }
    }

    fun fitToAircraft(aircraft: List<AircraftWithServers>) {
        val coordinates =
            aircraft
                .filter { it.aircraft.hasPosition }
                .map { it.aircraft.lat to it.aircraft.lon }
        when (val target = computeFitTarget(coordinates)) {
            null -> return
            is FitTarget.SinglePoint -> {
                viewModelScope.launch {
                    mapStateController.scrollTo(target.latitude, target.longitude, mapStateController.zoom)
                }
            }
            is FitTarget.BoundingRegion -> {
                viewModelScope.launch {
                    mapStateController.scrollTo(
                        bounds =
                            MapBounds(
                                north = target.north,
                                south = target.south,
                                east = target.east,
                                west = target.west,
                            ),
                        padding = Offset(x = 0.15f, y = 0.15f),
                    )
                }
            }
        }
    }

    fun toggleFollowUserLocation() {
        _followingUserLocation.value = !_followingUserLocation.value
    }

    internal fun onMapTouchDown() {
        if (_followingUserLocation.value) _followingUserLocation.value = false
    }

    fun onUserLocationChanged(location: Location) {
        if (_followingUserLocation.value) recenterOnLocation(location)
        mapStateController.removeMarker(USER_LOCATION_MARKER_ID)
        mapStateController.addMarker(
            id = USER_LOCATION_MARKER_ID,
            latitude = location.latitude,
            longitude = location.longitude,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_user_location),
                contentDescription = stringResource(Res.string.user_location_content_description),
                modifier = Modifier.size(24.dp),
            )
        }
    }

    fun onAircraftUpdated(aircraft: List<AircraftWithServers>) {
        previousAircraftMarkerIds.forEach(mapStateController::removeMarker)
        previousAircraftMarkerIds.clear()

        var followedLatLon: LatLon? = null
        val followingHex = _followingAircraft.value

        aircraft.forEach { item ->
            val plane = item.aircraft
            if (plane.lat == null || plane.lon == null) return@forEach

            if (followingHex != null && plane.hex == followingHex) {
                followedLatLon = LatLon(plane.lat, plane.lon)
            }

            mapStateController.addMarker(
                id = plane.hex.also { previousAircraftMarkerIds.add(it) },
                latitude = plane.lat,
                longitude = plane.lon,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_plane),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp).rotate(plane.track ?: 0f),
                    colorFilter = ColorFilter.tint(getColorForAltitude(plane.altBaro)),
                )
            }
        }

        followedLatLon?.let { latLon ->
            viewModelScope.launch {
                mapStateController.scrollTo(latLon.latitude, latLon.longitude, mapStateController.zoom)
            }
        }

        if (followingHex != null && followedLatLon == null) _followingAircraft.value = null
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
                    if (selectedHex != null) trails.filterKeys { it == selectedHex } else emptyMap()
                }
            }

        trailsToDisplay.forEach { (hex, trail) -> drawTrail(hex, trail) }
    }

    private fun drawTrail(
        hex: String,
        trail: AircraftTrail,
    ) {
        if (trail.positions.size >= 2) {
            val colorSegments = groupPositionsByAltitudeColor(trail.positions)
            colorSegments.forEachIndexed { index, segment ->
                if (segment.positions.size >= 2) {
                    val id = "trail_${hex}_$index"
                    previousPathIds.add(id)
                    mapStateController.addPath(
                        id = id,
                        color = segment.color,
                        width = 1.5.dp,
                        points = segment.positions.map { LatLon(it.latitude, it.longitude) },
                    )
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
                segments.add(currentSegment)
                currentColor = posColor
                currentSegment = ColorSegment(currentColor, mutableListOf(positions[i - 1], pos))
            }
        }
        segments.add(currentSegment)
        return segments
    }

    private fun clearPaths() {
        previousPathIds.forEach { mapStateController.removePath(it) }
        previousPathIds.clear()
    }
}
