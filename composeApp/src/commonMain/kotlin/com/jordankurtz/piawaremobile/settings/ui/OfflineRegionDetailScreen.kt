package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.extensions.formattedDate
import com.jordankurtz.piawaremobile.map.MapLibreMap
import com.jordankurtz.piawaremobile.map.MapLibreStateController
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.model.LatLon
import com.jordankurtz.piawaremobile.map.model.MapBounds
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.offline_maps_detail_created
import piawaremobile.composeapp.generated.resources.offline_maps_detail_provider
import piawaremobile.composeapp.generated.resources.offline_maps_detail_size
import piawaremobile.composeapp.generated.resources.offline_maps_detail_status
import piawaremobile.composeapp.generated.resources.offline_maps_detail_tile_count
import piawaremobile.composeapp.generated.resources.offline_maps_detail_zoom
import kotlin.time.Instant

private const val DETAIL_BOUNDS_PATH_ID = "detail_bounds"

@Composable
fun OfflineRegionDetailScreen(
    region: OfflineRegion,
    onBack: () -> Unit,
    mapViewModel: MapViewModel = koinViewModel(),
) {
    val activeProvider by mapViewModel.activeProvider.collectAsState()
    val boundsPathColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(region.id) {
        val bounds =
            MapBounds(
                north = region.maxLat,
                south = region.minLat,
                east = region.maxLon,
                west = region.minLon,
            )
        mapViewModel.mapStateController.scrollTo(bounds = bounds, padding = Offset(0.15f, 0.15f))
        mapViewModel.mapStateController.addPath(
            id = DETAIL_BOUNDS_PATH_ID,
            color = boundsPathColor,
            width = 2.dp,
            points =
                listOf(
                    LatLon(region.maxLat, region.minLon),
                    LatLon(region.maxLat, region.maxLon),
                    LatLon(region.minLat, region.maxLon),
                    LatLon(region.minLat, region.minLon),
                    LatLon(region.maxLat, region.minLon),
                ),
        )
    }
    DisposableEffect(region.id) {
        onDispose {
            mapViewModel.mapStateController.removePath(DETAIL_BOUNDS_PATH_ID)
        }
    }
    OfflineRegionDetailContent(
        region = region,
        mapLayer = {
            MapLibreMap(
                controller = mapViewModel.mapStateController as MapLibreStateController,
                styleUrl = activeProvider.styleUrl,
                modifier = Modifier.fillMaxSize(),
            )
        },
        onBack = onBack,
    )
}

@Composable
internal fun OfflineRegionDetailContent(
    region: OfflineRegion,
    mapLayer: @Composable () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopAppBar(
                title = region.name,
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                mapLayer()
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
            ) {
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_status),
                    value = region.status.name.lowercase().replaceFirstChar { it.uppercase() },
                )
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_zoom),
                    value = "${region.minZoom} – ${region.maxZoom}",
                )
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_tile_count),
                    value = region.tileCount.toString(),
                )
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_size),
                    value = "${region.sizeBytes / (1024 * 1024)} MB",
                )
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_provider),
                    value = region.providerId,
                )
                DetailRow(
                    label = stringResource(Res.string.offline_maps_detail_created),
                    value = Instant.fromEpochMilliseconds(region.createdAt).formattedDate,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}
