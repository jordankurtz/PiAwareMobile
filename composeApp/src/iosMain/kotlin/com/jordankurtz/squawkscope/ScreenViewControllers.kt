package com.jordankurtz.squawkscope

import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.squawkscope.map.MapScreen
import com.jordankurtz.squawkscope.map.MapViewModel
import com.jordankurtz.squawkscope.ui.Theme
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
