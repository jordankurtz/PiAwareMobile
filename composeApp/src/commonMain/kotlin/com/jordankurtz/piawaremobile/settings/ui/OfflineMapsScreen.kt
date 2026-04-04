package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.map.offline.MapRegionPickerScreen
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_add
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.ic_delete
import piawaremobile.composeapp.generated.resources.navigate_back
import piawaremobile.composeapp.generated.resources.offline_maps_add_region
import piawaremobile.composeapp.generated.resources.offline_maps_empty_message
import piawaremobile.composeapp.generated.resources.offline_maps_empty_title
import piawaremobile.composeapp.generated.resources.offline_maps_region_delete
import piawaremobile.composeapp.generated.resources.offline_maps_region_size
import piawaremobile.composeapp.generated.resources.offline_maps_region_zoom
import piawaremobile.composeapp.generated.resources.offline_maps_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsScreen(
    onBack: () -> Unit,
    regions: List<OfflineRegion> = emptyList(),
    onDeleteRegion: (OfflineRegion) -> Unit = {},
) {
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var pendingBounds by remember { mutableStateOf<BoundingBox?>(null) }

    if (showMapPicker) {
        MapRegionPickerScreen(
            onRegionSelected = { bounds ->
                pendingBounds = bounds
                showMapPicker = false
                showDownloadDialog = true
            },
            onDismiss = {
                showMapPicker = false
                showDownloadDialog = true
            },
        )
        return
    }

    if (showDownloadDialog) {
        DownloadRegionDialog(
            onDismiss = {
                showDownloadDialog = false
                pendingBounds = null
            },
            onConfirm = { _, _, _ ->
                showDownloadDialog = false
                pendingBounds = null
            },
            selectedBounds = pendingBounds,
            onSelectOnMap = {
                showDownloadDialog = false
                showMapPicker = true
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.offline_maps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                actions = {
                    IconButton(
                        onClick = { showDownloadDialog = true },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.offline_maps_add_region),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            if (regions.isEmpty()) {
                OfflineMapsEmptyState()
            } else {
                OfflineRegionList(
                    regions = regions,
                    onDeleteRegion = onDeleteRegion,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun OfflineMapsEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.offline_maps_empty_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(Res.string.offline_maps_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OfflineRegionList(
    regions: List<OfflineRegion>,
    onDeleteRegion: (OfflineRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(regions, key = { it.id }) { region ->
            OfflineRegionItem(
                region = region,
                onDelete = { onDeleteRegion(region) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun OfflineRegionItem(
    region: OfflineRegion,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = region.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.offline_maps_region_zoom, region.minZoom, region.maxZoom),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(
                        Res.string.offline_maps_region_size,
                        (region.sizeBytes / (1024 * 1024)).toInt(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.offline_maps_region_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
