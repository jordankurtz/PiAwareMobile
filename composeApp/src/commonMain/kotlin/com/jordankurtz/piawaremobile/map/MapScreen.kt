package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.Overlay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen() {
    val viewModel = koinViewModel<MapViewModel>()

    Box {
        OsmCommonUi(viewModel)
        Overlay(viewModel, modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp))
    }
}