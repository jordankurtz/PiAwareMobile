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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMap
import com.jordankurtz.piawaremobile.map.doProjection
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ovh.plrapps.mapcompose.api.BoundingBox
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.navigate_back
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
    LaunchedEffect(region.id) {
        val (xLeft, yTop) = doProjection(region.maxLat, region.minLon)
        val (xRight, yBottom) = doProjection(region.minLat, region.maxLon)
        val mapBounds = BoundingBox(xLeft = xLeft, yTop = yTop, xRight = xRight, yBottom = yBottom)
        mapViewModel.mapStateController.scrollTo(area = mapBounds, padding = Offset(0.15f, 0.15f))
        mapViewModel.mapStateController.addPath(
            id = DETAIL_BOUNDS_PATH_ID,
            color = Color(0xFF2196F3),
            width = 2.dp,
        ) {
            addPoints(
                listOf(
                    Pair(xLeft, yTop),
                    Pair(xRight, yTop),
                    Pair(xRight, yBottom),
                    Pair(xLeft, yBottom),
                    Pair(xLeft, yTop),
                ),
            )
        }
    }
    DisposableEffect(region.id) {
        onDispose {
            mapViewModel.mapStateController.removePath(DETAIL_BOUNDS_PATH_ID)
        }
    }
    OfflineRegionDetailContent(
        region = region,
        mapLayer = {
            OpenStreetMap(
                state = mapViewModel.state,
                modifier = Modifier.fillMaxSize(),
            )
        },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfflineRegionDetailContent(
    region: OfflineRegion,
    mapLayer: @Composable () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(region.name) },
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
                    ),
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
                    value = formatEpochDate(region.createdAt),
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

private fun formatEpochDate(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    @Suppress("DEPRECATION")
    return "${local.year}-${local.monthNumber.toString().padStart(
        2,
        '0',
    )}-${local.dayOfMonth.toString().padStart(2, '0')}"
}
