package com.jordankurtz.piawaremobile.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun OpenStreetMap(
    state: MapState,
    modifier: Modifier = Modifier
) {
    MapUI(
        state = state,
        modifier = modifier
    )
}
