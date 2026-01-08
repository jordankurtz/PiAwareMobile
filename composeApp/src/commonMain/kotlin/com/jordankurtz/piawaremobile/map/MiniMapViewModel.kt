package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
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
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftTrailUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.annotation.Factory
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.disableGestures
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_user_location
import kotlin.math.max
import kotlin.math.min

@Factory
class MiniMapViewModel(
    private val mapProvider: TileStreamProvider,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val getAircraftTrailUseCase: GetAircraftTrailUseCase
) : ViewModel() {

    private val previousPathIds = mutableSetOf<String>()
    private var settings: Settings? = null
    private var currentAircraft: Aircraft? = null
    private var currentLocation: Location? = null
    private var trailJob: Job? = null

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 4).apply {
        addLayer(mapProvider)
        setScrollOffsetRatio(xRatio = 0.5f, yRatio = 0.5f)
        disableGestures()
    }

    init {
        viewModelScope.launch {
            loadSettingsUseCase().collect {
                when (it) {
                    is Async.Success -> {
                        settings = it.data
                    }
                    is Async.Error -> {
                        Logger.e("Failed to load settings in MiniMapViewModel", it.throwable)
                    }
                    else -> {
                        // No-op
                    }
                }
            }
        }
    }

    fun updateMapState(aircraft: Aircraft?, location: Location?) {
        currentAircraft = aircraft
        currentLocation = location

        updateMarkersAndScroll()

        // Subscribe to trail updates for this aircraft
        trailJob?.cancel()
        trailJob = aircraft?.hex?.let { hex ->
            viewModelScope.launch {
                getAircraftTrailUseCase(hex).collect { trail ->
                    updateTrail(trail)
                }
            }
        }
    }

    private fun updateMarkersAndScroll() {
        viewModelScope.launch {
            val aircraft = currentAircraft
            val location = currentLocation

            state.removeMarker(id = "aircraft")
            state.removeMarker(id = "user_location")

            val aircraftLat = aircraft?.lat
            val aircraftLon = aircraft?.lon
            if (aircraftLat != null && aircraftLon != null) {
                val (x, y) = doProjection(latitude = aircraftLat, longitude = aircraftLon)
                state.addMarker(
                    id = "aircraft",
                    x = x,
                    y = y,
                    relativeOffset = Offset(-0.5f, -0.5f)
                ) {
                    Image(
                        painter = painterResource(resource = Res.drawable.ic_plane),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size = 30.dp)
                            .rotate(degrees = aircraft.track ?: 0f),
                        colorFilter = ColorFilter.tint(color = getColorForAltitude(altitude = aircraft.altBaro))
                    )
                }
            }

            val userLat = location?.latitude
            val userLon = location?.longitude
            if (userLat != null && userLon != null) {
                val (x, y) = doProjection(latitude = userLat, longitude = userLon)
                state.addMarker(
                    id = "user_location",
                    x = x,
                    y = y,
                    relativeOffset = Offset(-0.0f, -0.5f)
                ) {
                    Image(
                        painter = painterResource(resource = Res.drawable.ic_user_location),
                        contentDescription = null,
                        modifier = Modifier.size(size = 24.dp)
                    )
                }
            }

            if (aircraftLat != null && aircraftLon != null && userLat != null && userLon != null) {
                val (aircraftX, aircraftY) = doProjection(latitude = aircraftLat, longitude = aircraftLon)
                val (userX, userY) = doProjection(latitude = userLat, longitude = userLon)
                val boundingBox = BoundingBox(
                    xLeft = min(aircraftX, userX),
                    yTop = min(aircraftY, userY),
                    xRight = max(aircraftX, userX),
                    yBottom = max(aircraftY, userY)
                )
                state.scrollTo(
                    area = boundingBox,
                    padding = Offset(x = 0.2f, y = 0.2f),
                    animationSpec = SpringSpec(stiffness = Spring.StiffnessLow)
                )
            } else if (aircraftLat != null && aircraftLon != null) {
                val (x, y) = doProjection(latitude = aircraftLat, longitude = aircraftLon)
                state.scrollTo(x = x, y = y)
                state.scale = 0.1
            }
        }
    }

    private fun updateTrail(trail: AircraftTrail?) {
        viewModelScope.launch {
            previousPathIds.forEach { state.removePath(it) }
            previousPathIds.clear()

            if (settings?.showMinimapTrails == true) {
                trail?.let { t ->
                    if (t.positions.size >= 2) {
                        val colorSegments = groupPositionsByAltitudeColor(t.positions)

                        colorSegments.forEachIndexed { index, segment ->
                            if (segment.positions.size >= 2) {
                                val id = "trail_$index"
                                previousPathIds.add(id)

                                val projectedPoints = segment.positions.map { pos ->
                                    doProjection(pos.latitude, pos.longitude)
                                }

                                state.addPath(id, color = segment.color, width = 1.5.dp) {
                                    addPoints(projectedPoints)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private data class ColorSegment(
        val color: Color,
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
                segments.add(currentSegment)
                currentColor = posColor
                currentSegment = ColorSegment(currentColor, mutableListOf(positions[i - 1], pos))
            }
        }

        segments.add(currentSegment)
        return segments
    }
}
