package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_add
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.navigate_back
import piawaremobile.composeapp.generated.resources.server_add
import piawaremobile.composeapp.generated.resources.server_delete_confirm
import piawaremobile.composeapp.generated.resources.server_delete_confirm_message
import piawaremobile.composeapp.generated.resources.server_delete_confirm_title
import piawaremobile.composeapp.generated.resources.server_dialog_cancel
import piawaremobile.composeapp.generated.resources.servers_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<Server?>(null) }
    var deletingServer by remember { mutableStateOf<Server?>(null) }
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, address, type ->
                viewModel.addServer(name, address, type)
                showAddDialog = false
            },
        )
    }

    editingServer?.let { server ->
        EditServerDialog(
            server = server,
            onDismiss = { editingServer = null },
            onConfirm = { name, address, type ->
                viewModel.editServer(server.copy(name = name, address = address, type = type))
                editingServer = null
            },
        )
    }

    deletingServer?.let { server ->
        AlertDialog(
            onDismissRequest = { deletingServer = null },
            title = { Text(stringResource(Res.string.server_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.server_delete_confirm_message, server.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServer(server.id)
                        deletingServer = null
                    },
                ) {
                    Text(
                        stringResource(Res.string.server_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingServer = null }) {
                    Text(stringResource(Res.string.server_dialog_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.servers_title)) },
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
                        onClick = { showAddDialog = true },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.server_add),
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
            when (settings) {
                is Async.Error -> Text(text = settings.message)
                Async.Loading, Async.NotStarted -> CircularProgressIndicator()
                is Async.Success ->
                    ServerList(
                        servers = settings.data.servers,
                        onEditServer = { editingServer = it },
                        onDeleteServer = { deletingServer = it },
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}
