package com.jordankurtz.piawaremobile.aircraft.cache.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.aircraft.cache.CachedRegion
import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheViewModel
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.map.offline.MapRegionPickerScreen
import com.jordankurtz.piawaremobile.model.Async
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.flight_cache_add_region
import piawaremobile.composeapp.generated.resources.flight_cache_delete_confirm_cancel
import piawaremobile.composeapp.generated.resources.flight_cache_delete_confirm_delete
import piawaremobile.composeapp.generated.resources.flight_cache_delete_confirm_message
import piawaremobile.composeapp.generated.resources.flight_cache_delete_confirm_title
import piawaremobile.composeapp.generated.resources.flight_cache_empty_message
import piawaremobile.composeapp.generated.resources.flight_cache_empty_title
import piawaremobile.composeapp.generated.resources.flight_cache_region_days
import piawaremobile.composeapp.generated.resources.flight_cache_region_delete
import piawaremobile.composeapp.generated.resources.flight_cache_region_flights
import piawaremobile.composeapp.generated.resources.flight_cache_title
import piawaremobile.composeapp.generated.resources.ic_add
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.ic_delete
import piawaremobile.composeapp.generated.resources.navigate_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightCacheScreen(
    onBack: () -> Unit,
    regions: List<CachedRegion> = emptyList(),
    prefetchState: Async<Int> = Async.NotStarted,
    onStartPrefetch: (name: String, box: BoundingBox, daysAhead: Int) -> Unit = { _, _, _ -> },
    onDeleteRegion: (CachedRegion) -> Unit = {},
    onResetPrefetchState: () -> Unit = {},
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var pendingBox by remember { mutableStateOf<BoundingBox?>(null) }
    var pendingDeleteRegion by remember { mutableStateOf<CachedRegion?>(null) }

    if (showMapPicker) {
        MapRegionPickerScreen(
            onRegionSelected = { box, _ ->
                pendingBox = box
                showMapPicker = false
                showAddSheet = true
            },
            onDismiss = {
                showMapPicker = false
                showAddSheet = true
            },
        )
        return
    }

    pendingDeleteRegion?.let { region ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRegion = null },
            title = { Text(stringResource(Res.string.flight_cache_delete_confirm_title)) },
            text = {
                Text(stringResource(Res.string.flight_cache_delete_confirm_message, region.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRegion(region)
                    pendingDeleteRegion = null
                }) { Text(stringResource(Res.string.flight_cache_delete_confirm_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRegion = null }) {
                    Text(stringResource(Res.string.flight_cache_delete_confirm_cancel))
                }
            },
        )
    }

    if (showAddSheet) {
        FlightCacheAddSheet(
            onDismiss = {
                showAddSheet = false
                onResetPrefetchState()
            },
            onSelectRegion = {
                showAddSheet = false
                showMapPicker = true
            },
            onConfirm = { name, box, days ->
                onStartPrefetch(name, box, days)
                showAddSheet = false
            },
            pendingBox = pendingBox,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.flight_cache_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.flight_cache_add_region),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (prefetchState is Async.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            if (regions.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.flight_cache_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.flight_cache_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn {
                    items(regions, key = { it.id }) { region ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(region.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(Res.string.flight_cache_region_flights, region.flightCount),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    stringResource(Res.string.flight_cache_region_days, region.daysAhead),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { pendingDeleteRegion = region }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_delete),
                                    contentDescription = stringResource(Res.string.flight_cache_region_delete),
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun FlightCacheScreenContainer(onBack: () -> Unit) {
    val vm: FlightCacheViewModel = koinViewModel()
    val regions by vm.regions.collectAsState()
    val prefetchState by vm.prefetchState.collectAsState()
    FlightCacheScreen(
        onBack = onBack,
        regions = regions,
        prefetchState = prefetchState,
        onStartPrefetch = { name, box, days -> vm.startPrefetch(name, box, days) },
        onDeleteRegion = { vm.deleteRegion(it.id) },
        onResetPrefetchState = { vm.resetPrefetchState() },
    )
}
