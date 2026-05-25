package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftTrailUseCase
import com.jordankurtz.piawaremobile.map.model.LatLon
import com.jordankurtz.piawaremobile.map.model.MapBounds
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
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_user_location
import kotlin.math.max
import kotlin.math.min

@Factory
class MiniMapViewModel(
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val getAircraftTrailUseCase: GetAircraftTrailUseCase,
    internal val mapStateController: MapStateController,
) : ViewModel() {
    private val previousPathIds = mutableSetOf<String>()
    private var settings: Settings? = null
    private var currentAircraft: Aircraft? = null
    private var currentLocation: Location? = null
    private var trailJob: Job? = null

    init {
        viewModelScope.launch {
            loadSettingsUseCase().collect {
                when (it) {
                    is Async.Success -> settings = it.data
                    is Async.Error -> Logger.e("Failed to load settings in MiniMapViewModel", it.throwable)
                    else -> Unit
                }
            }
        }
    }

    fun updateMapState(
        aircraft: Aircraft?,
        location: Location?,
    ) {
        currentAircraft = aircraft
        currentLocation = location
        updateMarkersAndScroll()
        trailJob?.cancel()
        trailJob =
            aircraft?.hex?.let { hex ->
                viewModelScope.launch {
                    getAircraftTrailUseCase(hex).collect { trail -> updateTrail(trail) }
                }
            }
    }

    private fun updateMarkersAndScroll() {
        viewModelScope.launch {
            mapStateController.removeMarker("aircraft")
            mapStateController.removeMarker("user_location")

            val aircraft = currentAircraft
            val location = currentLocation
            val aircraftLat = aircraft?.lat?.takeIf { aircraft.hasPosition }
            val aircraftLon = aircraft?.lon?.takeIf { aircraft.hasPosition }

            if (aircraftLat != null && aircraftLon != null) {
                mapStateController.addMarker("aircraft", aircraftLat, aircraftLon) {
                    Image(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).rotate(aircraft.track ?: 0f),
                        colorFilter = ColorFilter.tint(getColorForAltitude(aircraft.altBaro)),
                    )
                }
            }

            val userLat = location?.latitude
            val userLon = location?.longitude
            if (userLat != null && userLon != null) {
                mapStateController.addMarker("user_location", userLat, userLon) {
                    Image(
                        painter = painterResource(Res.drawable.ic_user_location),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            if (aircraftLat != null && aircraftLon != null && userLat != null && userLon != null) {
                val latPad = max(aircraftLat, userLat) - min(aircraftLat, userLat)
                val lonPad = max(aircraftLon, userLon) - min(aircraftLon, userLon)
                mapStateController.scrollTo(
                    bounds =
                        MapBounds(
                            north = max(aircraftLat, userLat) + latPad * 0.2,
                            south = min(aircraftLat, userLat) - latPad * 0.2,
                            east = max(aircraftLon, userLon) + lonPad * 0.2,
                            west = min(aircraftLon, userLon) - lonPad * 0.2,
                        ),
                    padding = Offset(0.2f, 0.2f),
                    animationSpec = SpringSpec(stiffness = Spring.StiffnessLow),
                )
            } else if (aircraftLat != null && aircraftLon != null) {
                mapStateController.scrollTo(aircraftLat, aircraftLon, 14.0)
            }
        }
    }

    private fun updateTrail(trail: AircraftTrail?) {
        viewModelScope.launch {
            previousPathIds.forEach { mapStateController.removePath(it) }
            previousPathIds.clear()

            if (settings?.showMinimapTrails == true) {
                trail?.let { t ->
                    if (t.positions.size >= 2) {
                        val colorSegments = groupPositionsByAltitudeColor(t.positions)
                        colorSegments.forEachIndexed { index, segment ->
                            if (segment.positions.size >= 2) {
                                val id = "trail_$index"
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
}
