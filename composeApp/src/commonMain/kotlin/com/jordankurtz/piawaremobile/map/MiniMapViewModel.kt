package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.model.Aircraft
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.annotation.Factory
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.disableGestures
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane

@Factory
class MiniMapViewModel(
    private val mapProvider: TileStreamProvider
) : ViewModel() {

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 4).apply {
        addLayer(mapProvider)
        scale = 0.1 // Set a default zoom level
        setScrollOffsetRatio(0.5f, 0.5f)
        disableGestures()
    }

    fun setAircraft(aircraft: Aircraft?) {
        viewModelScope.launch {
            state.removeAllMarkers()

            if (aircraft != null) {
                val (x, y) = doProjection(aircraft.lat, aircraft.lon)
                state.addMarker(
                    id = aircraft.hex,
                    x = x,
                    y = y,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .rotate(aircraft.track ?: 0f),
                        colorFilter = ColorFilter.tint(getColorForAltitude(aircraft.altBaro))
                    )
                }
                state.scrollTo(x, y)
            }
        }
    }
}
