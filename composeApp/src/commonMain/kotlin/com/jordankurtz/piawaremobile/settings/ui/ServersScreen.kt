package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_add
import piawaremobile.composeapp.generated.resources.servers_title

@Composable
fun ServersScreen(onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState

    if (showDialog) {
        AddServerDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, address ->
                viewModel.addServer(name, address)
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.servers_title)) },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White,
                actions = {
                    IconButton(
                        { showDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            )
        }
    ) {
        when (settings) {
            is Async.Error -> Text(text = settings.message)
            Async.Loading, Async.NotStarted -> CircularProgressIndicator()
            is Async.Success -> ServerList(settings.data.servers)
        }
    }
}
