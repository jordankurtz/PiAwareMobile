package com.jordankurtz.piawaremobile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.jordankurtz.piawaremobile.map.offline.OfflineMapsViewModel
import com.jordankurtz.piawaremobile.settings.ui.FlightCacheScreen
import com.jordankurtz.piawaremobile.settings.ui.MapProvidersScreen
import com.jordankurtz.piawaremobile.settings.ui.OfflineMapsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.koin.compose.viewmodel.koinViewModel

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

fun FlightCacheViewController() =
    ComposeUIViewController {
        Theme {
            FlightCacheScreen(onBack = {})
        }
    }

fun MapProvidersViewController() =
    ComposeUIViewController {
        Theme {
            MapProvidersScreen(onBack = {})
        }
    }
