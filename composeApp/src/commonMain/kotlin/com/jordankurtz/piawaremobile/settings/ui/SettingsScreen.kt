package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.offline_maps_delete_confirm_cancel
import piawaremobile.composeapp.generated.resources.offline_maps_delete_confirm_delete
import piawaremobile.composeapp.generated.resources.offline_maps_delete_confirm_message
import piawaremobile.composeapp.generated.resources.offline_maps_delete_confirm_title

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
                    val pendingDelete by vm.pendingDeleteRegion.collectAsState()
                    val pendingDeleteFreedBytes by vm.pendingDeleteFreedBytes.collectAsState()
                    val onRequestDelete = remember(vm) { { region: OfflineRegion -> vm.requestDeleteRegion(region) } }
                    val onConfirmDelete = remember(vm) { { vm.confirmDelete() } }
                    val onCancelDelete = remember(vm) { { vm.cancelDelete() } }
                    val onCancelDownload = remember(vm) { { vm.cancelDownload() } }
                    val onStartDownloadFn =
                        remember(vm) {
                            { name: String, bounds: BoundingBox, minZoom: Int, maxZoom: Int ->
                                vm.startDownload(name, bounds, minZoom, maxZoom)
                            }
                        }
                    pendingDelete?.let { region ->
                        AlertDialog(
                            onDismissRequest = onCancelDelete,
                            title = { Text(stringResource(Res.string.offline_maps_delete_confirm_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        Res.string.offline_maps_delete_confirm_message,
                                        region.name,
                                        (pendingDeleteFreedBytes / (1024 * 1024)).toInt(),
                                    ),
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = onConfirmDelete) {
                                    Text(stringResource(Res.string.offline_maps_delete_confirm_delete))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = onCancelDelete) {
                                    Text(stringResource(Res.string.offline_maps_delete_confirm_cancel))
                                }
                            },
                        )
                    }
                    OfflineMapsScreen(
                        onBack = { currentScreen = SettingsScreens.Main },
                        regions = regions,
                        onDeleteRegion = onRequestDelete,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onStartDownload = onStartDownloadFn,
                        onCancelDownload = onCancelDownload,
                    )
                }
            }
        }
    }
}
