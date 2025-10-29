package com.jordankurtz.piawaremobile.map

import androidx.compose.runtime.Composable
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun OpenStreetMap(
    state: MapState
) {
    MapUI(
        state = state,
    )
}
