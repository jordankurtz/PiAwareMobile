package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.map.offline.OfflineMapsViewModel
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import com.jordankurtz.piawaremobile.settings.SettingsScreens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen() {
    var currentScreen by remember { mutableStateOf<SettingsScreens>(SettingsScreens.Main) }
    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { slideInHorizontally() togetherWith slideOutHorizontally() },
        ) { screen ->
            when (screen) {
                SettingsScreens.Main ->
                    MainScreen(
                        onServersClicked = { currentScreen = SettingsScreens.Servers },
                        onOfflineMapsClicked = { currentScreen = SettingsScreens.OfflineMaps },
                    )
                SettingsScreens.Servers -> ServersScreen(onBack = {})
                SettingsScreens.OfflineMaps -> {
                    val vm: OfflineMapsViewModel = koinViewModel()
                    val regions by vm.regions.collectAsState()
                    val isDownloading by vm.isDownloading.collectAsState()
                    val downloadProgress by vm.downloadProgress.collectAsState()
                    val onDeleteRegion = remember(vm) { { region: OfflineRegion -> vm.deleteRegion(region.id) } }
                    val onStartDownloadFn =
                        remember(vm) {
                            { name: String, bounds: BoundingBox, minZoom: Int, maxZoom: Int ->
                                vm.startDownload(name, bounds, minZoom, maxZoom)
                            }
                        }
                    OfflineMapsScreen(
                        onBack = { currentScreen = SettingsScreens.Main },
                        regions = regions,
                        onDeleteRegion = onDeleteRegion,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onStartDownload = onStartDownloadFn,
                    )
                }
            }
        }
    }
}
