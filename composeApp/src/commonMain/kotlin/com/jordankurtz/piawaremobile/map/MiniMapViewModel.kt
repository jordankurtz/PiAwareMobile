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
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Location
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.annotation.Factory
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.disableGestures
import ovh.plrapps.mapcompose.api.removeMarker
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
    private val mapProvider: TileStreamProvider
) : ViewModel() {

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 4).apply {
        addLayer(mapProvider)
        setScrollOffsetRatio(xRatio = 0.5f, yRatio = 0.5f)
        disableGestures()
    }

    fun updateMapState(aircraft: Aircraft?, location: Location?) {
        viewModelScope.launch {
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
                    y = y
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
}
