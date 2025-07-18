package com.jordankurtz.piawareviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawareviewer.map.MapViewModel
import com.jordankurtz.piawareviewer.map.OsmCommonUi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication
import org.koin.dsl.module

@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<MapViewModel>()
    Box {
        OsmCommonUi(viewModel)
        Overlay(viewModel, modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp))
    }
}

