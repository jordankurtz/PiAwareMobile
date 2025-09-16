package com.jordankurtz.piawaremobile.map

import androidx.compose.runtime.Composable
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun OsmCommonUi(screenModel: MapViewModel) {
    MapUI(
        state = screenModel.state
    )
}