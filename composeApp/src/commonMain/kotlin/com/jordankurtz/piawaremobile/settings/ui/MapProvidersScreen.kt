package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.map.toTileProviderConfig
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.map_providers_add_custom
import piawaremobile.composeapp.generated.resources.map_providers_api_key_configured
import piawaremobile.composeapp.generated.resources.map_providers_api_key_hint
import piawaremobile.composeapp.generated.resources.map_providers_api_key_required
import piawaremobile.composeapp.generated.resources.map_providers_cancel
import piawaremobile.composeapp.generated.resources.map_providers_custom_name_hint
import piawaremobile.composeapp.generated.resources.map_providers_custom_url_hint
import piawaremobile.composeapp.generated.resources.map_providers_delete_custom
import piawaremobile.composeapp.generated.resources.map_providers_generic_key_info
import piawaremobile.composeapp.generated.resources.map_providers_jawg_key_info
import piawaremobile.composeapp.generated.resources.map_providers_save
import piawaremobile.composeapp.generated.resources.map_providers_stadia_key_info
import piawaremobile.composeapp.generated.resources.map_providers_thunderforest_key_info
import piawaremobile.composeapp.generated.resources.map_providers_title
import piawaremobile.composeapp.generated.resources.map_providers_url_preview
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun MapProvidersScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = (settingsState as? Async.Success)?.data
    val activeProviderId = settings?.mapProviderId ?: TileProviders.OPENSTREETMAP.id
    val apiKeys = settings?.apiKeys ?: emptyMap()
    val customProviders = settings?.customProviders ?: emptyList()

    var pendingApiKeyProvider by remember { mutableStateOf<TileProviderConfig?>(null) }
    var showAddCustom by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.map_providers_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = null,
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
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(TileProviders.BUILT_IN) { config ->
                BuiltInProviderRow(
                    config = config,
                    isSelected = config.id == activeProviderId,
                    onClick = { viewModel.updateMapProvider(config) },
                )
            }

            items(TileProviders.API_KEY_REQUIRED) { config ->
                val keyLookup = config.apiKeyGroup ?: config.id
                ApiKeyProviderRow(
                    config = config,
                    isSelected = config.id == activeProviderId,
                    hasKey = apiKeys.containsKey(keyLookup),
                    onClick = {
                        if (apiKeys.containsKey(keyLookup)) {
                            viewModel.updateMapProvider(config)
                        } else {
                            pendingApiKeyProvider = config
                        }
                    },
                )
            }

            items(customProviders) { custom ->
                CustomProviderRow(
                    config = custom,
                    isSelected = custom.id == activeProviderId,
                    onClick = { viewModel.updateMapProvider(custom.toTileProviderConfig()) },
                    onDelete = { viewModel.deleteCustomProvider(custom.id) },
                )
            }

            item {
                TextButton(
                    onClick = { showAddCustom = true },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(Res.string.map_providers_add_custom))
                }
            }
        }
    }

    pendingApiKeyProvider?.let { provider ->
        val keyGroup = provider.apiKeyGroup ?: provider.id
        val providerName =
            when (provider.apiKeyGroup) {
                "stadia" -> "Stadia Maps"
                "thunderforest" -> "Thunderforest"
                "jawg" -> "Jawg"
                else -> provider.displayNameRes?.let { stringResource(it) } ?: provider.displayName
            }
        val keyInfo =
            when (provider.apiKeyGroup) {
                "stadia" -> stringResource(Res.string.map_providers_stadia_key_info)
                "thunderforest" -> stringResource(Res.string.map_providers_thunderforest_key_info)
                "jawg" -> stringResource(Res.string.map_providers_jawg_key_info)
                else -> stringResource(Res.string.map_providers_generic_key_info)
            }
        ApiKeyBottomSheet(
            providerName = providerName,
            keyInfo = keyInfo,
            onSave = { key ->
                viewModel.setApiKeyAndActivateProvider(keyGroup, key, provider)
                pendingApiKeyProvider = null
            },
            onDismiss = { pendingApiKeyProvider = null },
        )
    }

    if (showAddCustom) {
        AddCustomProviderBottomSheet(
            onSave = { name, urlTemplate ->
                viewModel.addCustomProvider(Uuid.random().toString(), name, urlTemplate)
                showAddCustom = false
            },
            onDismiss = { showAddCustom = false },
        )
    }
}

@Composable
fun BuiltInProviderRow(
    config: TileProviderConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = config.displayNameRes?.let { stringResource(it) } ?: config.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun ApiKeyProviderRow(
    config: TileProviderConfig,
    isSelected: Boolean,
    hasKey: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = config.displayNameRes?.let { stringResource(it) } ?: config.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (hasKey) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = stringResource(Res.string.map_providers_api_key_configured),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = stringResource(Res.string.map_providers_api_key_required),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun CustomProviderRow(
    config: CustomProviderConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = config.urlTemplate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(
                    text = stringResource(Res.string.map_providers_delete_custom),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        HorizontalDivider()
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            text = { Text("Delete \"${config.displayName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(Res.string.map_providers_delete_custom))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.map_providers_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyBottomSheet(
    providerName: String,
    keyInfo: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = providerName, style = MaterialTheme.typography.titleMedium)
            Text(text = keyInfo, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(Res.string.map_providers_api_key_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.map_providers_cancel))
                }
                Button(
                    onClick = { onSave(key) },
                    enabled = key.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.map_providers_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomProviderBottomSheet(
    onSave: (name: String, urlTemplate: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var urlTemplate by remember { mutableStateOf("") }
    val urlPreview =
        urlTemplate
            .replace("{z}", "10")
            .replace("{x}", "512")
            .replace("{y}", "512")
    val isValid =
        name.isNotBlank() &&
            urlTemplate.contains("{z}") &&
            urlTemplate.contains("{x}") &&
            urlTemplate.contains("{y}")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.map_providers_add_custom),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.map_providers_custom_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = urlTemplate,
                onValueChange = { urlTemplate = it },
                label = { Text(stringResource(Res.string.map_providers_custom_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (urlTemplate.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.map_providers_url_preview, urlPreview),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.map_providers_cancel))
                }
                Button(
                    onClick = { onSave(name, urlTemplate) },
                    enabled = isValid,
                ) {
                    Text(stringResource(Res.string.map_providers_save))
                }
            }
        }
    }
}
