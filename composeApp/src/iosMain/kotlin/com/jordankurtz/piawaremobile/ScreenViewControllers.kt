package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.ui.Theme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform

fun MapViewController(onMounted: (() -> Unit)? = null) =
    ComposeUIViewController {
        val mapViewModel: MapViewModel = koinViewModel()
        Theme {
            MapScreen(
                mapViewModel = mapViewModel,
                showFollowLocationFab = false,
                showNativeOverlays = true,
                onMounted = onMounted,
            )
        }
    }

fun toggleMapFollowUserLocation() {
    KoinPlatform.getKoin().get<MapViewModel>().toggleFollowUserLocation()
}

