package com.jordankurtz.piawaremobile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.list.AircraftListScreen
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.koin.compose.viewmodel.koinViewModel

private var mapToggleFn: () -> Unit = {}

fun MapViewController() =
    ComposeUIViewController {
        val mapViewModel: MapViewModel = koinViewModel()
        LaunchedEffect(mapViewModel) {
            mapToggleFn = mapViewModel::toggleFollowUserLocation
        }
        Theme {
            MapScreen(
                mapViewModel = mapViewModel,
                showFollowLocationFab = false,
            )
        }
    }

fun toggleMapFollowUserLocation() {
    mapToggleFn()
}

fun AircraftListViewController() =
    ComposeUIViewController {
        Theme {
            AircraftListScreen()
        }
    }

fun SettingsViewController() =
    ComposeUIViewController {
        Theme {
            SettingsScreen()
        }
    }
