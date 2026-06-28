package com.jordankurtz.piawaremobile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.offline.OfflineMapsViewModel
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.map.offline.MapRegionPickerScreen

import com.jordankurtz.piawaremobile.settings.ui.MapProvidersScreen
import com.jordankurtz.piawaremobile.settings.ui.OfflineMapsScreen
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

fun OfflineMapsViewController() =
    ComposeUIViewController {
        val viewModel: OfflineMapsViewModel = koinViewModel()
        val regions by viewModel.regions.collectAsState()
        Theme {
            OfflineMapsScreen(
                onBack = {},
                regions = regions,
                onDeleteRegion = { viewModel.requestDeleteRegion(it) },
                onRetry = { viewModel.retryDownload(it) },
                onStartDownload = { name, bounds, minZoom, maxZoom, viewportZoom ->
                    viewModel.startDownload(name, bounds, minZoom, maxZoom, viewportZoom)
                },
                onCancelDownload = { viewModel.cancelDownload() },
            )
        }
    }


fun MapProvidersViewController() =
    ComposeUIViewController {
        Theme {
            MapProvidersScreen(onBack = {})
        }
    }

fun MapRegionPickerViewController(
    onRegionSelected: (BoundingBox, Int) -> Unit,
    onDismiss: () -> Unit,
) = ComposeUIViewController {
    Theme {
        MapRegionPickerScreen(
            onRegionSelected = onRegionSelected,
            onDismiss = onDismiss,
        )
    }
}
