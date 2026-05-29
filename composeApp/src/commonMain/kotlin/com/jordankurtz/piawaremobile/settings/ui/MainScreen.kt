package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.app_section_title
import piawaremobile.composeapp.generated.resources.center_map_on_user_description
import piawaremobile.composeapp.generated.resources.center_map_on_user_title
import piawaremobile.composeapp.generated.resources.clear_map_cache_confirm_cancel
import piawaremobile.composeapp.generated.resources.clear_map_cache_confirm_clear
import piawaremobile.composeapp.generated.resources.clear_map_cache_confirm_message
import piawaremobile.composeapp.generated.resources.clear_map_cache_confirm_title
import piawaremobile.composeapp.generated.resources.clear_map_cache_description
import piawaremobile.composeapp.generated.resources.clear_map_cache_title
import piawaremobile.composeapp.generated.resources.enable_flightaware_api_description
import piawaremobile.composeapp.generated.resources.enable_flightaware_api_title
import piawaremobile.composeapp.generated.resources.flightaware_api_key_title
import piawaremobile.composeapp.generated.resources.flightaware_section_title
import piawaremobile.composeapp.generated.resources.ic_chevron_right
import piawaremobile.composeapp.generated.resources.map_provider_title
import piawaremobile.composeapp.generated.resources.map_section_title
import piawaremobile.composeapp.generated.resources.offline_maps_settings_title
import piawaremobile.composeapp.generated.resources.offline_section_title
import piawaremobile.composeapp.generated.resources.open_urls_externally_description
import piawaremobile.composeapp.generated.resources.open_urls_externally_title
import piawaremobile.composeapp.generated.resources.refresh_interval_title
import piawaremobile.composeapp.generated.resources.restore_map_position_description
import piawaremobile.composeapp.generated.resources.restore_map_position_title
import piawaremobile.composeapp.generated.resources.servers_section_title
import piawaremobile.composeapp.generated.resources.servers_title
import piawaremobile.composeapp.generated.resources.settings_title
import piawaremobile.composeapp.generated.resources.show_minimap_trails_description
import piawaremobile.composeapp.generated.resources.show_minimap_trails_title
import piawaremobile.composeapp.generated.resources.show_receiver_locations_description
import piawaremobile.composeapp.generated.resources.show_receiver_locations_title
import piawaremobile.composeapp.generated.resources.show_user_location_description
import piawaremobile.composeapp.generated.resources.show_user_location_title
import piawaremobile.composeapp.generated.resources.trail_display_mode_description
import piawaremobile.composeapp.generated.resources.trail_display_mode_title
import piawaremobile.composeapp.generated.resources.zoom_default_title
import piawaremobile.composeapp.generated.resources.zoom_max_title
import piawaremobile.composeapp.generated.resources.zoom_min_title

@Composable
fun MainScreen(
    onServersClicked: () -> Unit,
    onOfflineMapsClicked: () -> Unit = {},
    onMapProviderClicked: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(stringResource(Res.string.clear_map_cache_confirm_title)) },
            text = { Text(stringResource(Res.string.clear_map_cache_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearTileCache()
                    showClearCacheConfirm = false
                }) {
                    Text(stringResource(Res.string.clear_map_cache_confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text(stringResource(Res.string.clear_map_cache_confirm_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = { SettingsTopAppBar(title = stringResource(Res.string.settings_title)) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                val activeProviderId = settings.getValue()?.mapProviderId ?: TileProviders.DEFAULT.id
                val builtInMatch = TileProviders.ALL.find { it.id == activeProviderId }
                val providerDisplayName =
                    builtInMatch?.displayNameRes?.let { stringResource(it) }
                        ?: settings.getValue()?.customProviders?.find { it.id == activeProviderId }?.displayName
                        ?: activeProviderId

                SettingsGroup(title = stringResource(Res.string.map_section_title)) {
                    SettingsItem(
                        title = stringResource(Res.string.map_provider_title),
                        description = providerDisplayName,
                        onClick = onMapProviderClicked,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        title = stringResource(Res.string.center_map_on_user_title),
                        description = stringResource(Res.string.center_map_on_user_description),
                        checked = settings.getValue()?.centerMapOnUserOnStart ?: false,
                        onCheckedChange = viewModel::updateCenterMapOnUserOnStart,
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        title = stringResource(Res.string.restore_map_position_title),
                        description = stringResource(Res.string.restore_map_position_description),
                        checked = settings.getValue()?.restoreMapStateOnStart ?: true,
                        onCheckedChange = viewModel::updateRestoreMapStateOnStart,
                    )
                    HorizontalDivider()
                    SettingsDropdown(
                        title = stringResource(Res.string.trail_display_mode_title),
                        description = stringResource(Res.string.trail_display_mode_description),
                        selectedValue = settings.getValue()?.trailDisplayMode ?: TrailDisplayMode.ALL,
                        values = TrailDisplayMode.entries.toTypedArray(),
                        onValueSelected = viewModel::updateTrailDisplayMode,
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        title = stringResource(Res.string.show_minimap_trails_title),
                        description = stringResource(Res.string.show_minimap_trails_description),
                        checked = settings.getValue()?.showMinimapTrails ?: true,
                        onCheckedChange = viewModel::updateShowMinimapTrails,
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        title = stringResource(Res.string.show_receiver_locations_title),
                        description = stringResource(Res.string.show_receiver_locations_description),
                        checked = settings.getValue()?.showReceiverLocations ?: true,
                        onCheckedChange = viewModel::updateShowReceiverLocations,
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        title = stringResource(Res.string.show_user_location_title),
                        description = stringResource(Res.string.show_user_location_description),
                        checked = settings.getValue()?.showUserLocationOnMap ?: true,
                        onCheckedChange = viewModel::updateShowUserLocationOnMap,
                    )
                    HorizontalDivider()
                    SettingsNumberInput(
                        title = stringResource(Res.string.zoom_default_title),
                        value = settings.getValue()?.defaultZoomLevel ?: SettingsRepository.DEFAULT_ZOOM_LEVEL,
                        onValueChange = viewModel::updateDefaultZoomLevel,
                        range = SettingsRepository.MIN_ZOOM_LEVEL..SettingsRepository.MAX_ZOOM_LEVEL,
                    )
                    HorizontalDivider()
                    SettingsNumberInput(
                        title = stringResource(Res.string.zoom_min_title),
                        value = settings.getValue()?.minZoomLevel ?: SettingsRepository.MIN_ZOOM_LEVEL,
                        onValueChange = viewModel::updateMinZoomLevel,
                        range = SettingsRepository.MIN_ZOOM_LEVEL..SettingsRepository.MAX_ZOOM_LEVEL,
                    )
                    HorizontalDivider()
                    SettingsNumberInput(
                        title = stringResource(Res.string.zoom_max_title),
                        value = settings.getValue()?.maxZoomLevel ?: SettingsRepository.MAX_ZOOM_LEVEL,
                        onValueChange = viewModel::updateMaxZoomLevel,
                        range = SettingsRepository.MIN_ZOOM_LEVEL..SettingsRepository.MAX_ZOOM_LEVEL,
                    )
                    HorizontalDivider()
                    SettingsItem(
                        title = stringResource(Res.string.clear_map_cache_title),
                        description = stringResource(Res.string.clear_map_cache_description),
                        onClick = { showClearCacheConfirm = true },
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(Res.string.offline_section_title)) {
                    SettingsItem(
                        title = stringResource(Res.string.offline_maps_settings_title),
                        onClick = onOfflineMapsClicked,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(Res.string.servers_section_title)) {
                    SettingsItem(
                        title = stringResource(Res.string.servers_title),
                        onClick = onServersClicked,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    HorizontalDivider()
                    SettingsNumberInput(
                        title = stringResource(Res.string.refresh_interval_title),
                        value =
                            settings.getValue()?.refreshInterval
                                ?: SettingsRepository.DEFAULT_REFRESH_INTERVAL,
                        onValueChange = viewModel::updateRefreshInterval,
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(Res.string.flightaware_section_title)) {
                    SettingsSwitch(
                        title = stringResource(Res.string.enable_flightaware_api_title),
                        description = stringResource(Res.string.enable_flightaware_api_description),
                        checked = settings.getValue()?.enableFlightAwareApi ?: false,
                        onCheckedChange = viewModel::updateEnableFlightAwareApi,
                    )
                    HorizontalDivider()
                    SettingsTextInput(
                        title = stringResource(Res.string.flightaware_api_key_title),
                        value = settings.getValue()?.flightAwareApiKey ?: "",
                        onValueChange = viewModel::updateFlightAwareApiKey,
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(Res.string.app_section_title)) {
                    SettingsSwitch(
                        title = stringResource(Res.string.open_urls_externally_title),
                        description = stringResource(Res.string.open_urls_externally_description),
                        checked = settings.getValue()?.openUrlsExternally ?: false,
                        onCheckedChange = viewModel::updateOpenUrlsExternally,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    description: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailingIcon?.invoke()
    }
}

@Composable
fun <T> SettingsDropdown(
    title: String,
    description: String,
    selectedValue: T,
    values: Array<T>,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    stringFor: @Composable (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(stringFor(selectedValue))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(stringFor(value)) },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsNumberInput(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange? = null,
) {
    var textValue by remember { mutableStateOf(value.toString()) }
    val parsedValue = textValue.toIntOrNull()
    val isValid = parsedValue != null && (range == null || parsedValue in range)

    LaunchedEffect(value) {
        if (textValue.toIntOrNull() != value) {
            textValue = value.toString()
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        OutlinedTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                it.toIntOrNull()?.let { v ->
                    if (range == null || v in range) onValueChange(v)
                }
            },
            singleLine = true,
            modifier =
                Modifier
                    .width(80.dp)
                    .padding(start = 16.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
fun SettingsTextInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (textValue != value) {
            textValue = value
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onValueChange(it)
            },
            label = { Text(title) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
