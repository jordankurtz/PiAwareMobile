package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.server_dialog_add_title
import piawaremobile.composeapp.generated.resources.server_dialog_address_label
import piawaremobile.composeapp.generated.resources.server_dialog_address_required
import piawaremobile.composeapp.generated.resources.server_dialog_cancel
import piawaremobile.composeapp.generated.resources.server_dialog_edit_title
import piawaremobile.composeapp.generated.resources.server_dialog_name_label
import piawaremobile.composeapp.generated.resources.server_dialog_name_required
import piawaremobile.composeapp.generated.resources.server_dialog_save
import piawaremobile.composeapp.generated.resources.server_dialog_type_label
import piawaremobile.composeapp.generated.resources.server_dialog_type_piaware
import piawaremobile.composeapp.generated.resources.server_dialog_type_piaware_description
import piawaremobile.composeapp.generated.resources.server_dialog_type_readsb
import piawaremobile.composeapp.generated.resources.server_dialog_type_readsb_description

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, type: ServerType) -> Unit,
) {
    ServerDialog(
        title = stringResource(Res.string.server_dialog_add_title),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun EditServerDialog(
    server: Server,
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, type: ServerType) -> Unit,
) {
    ServerDialog(
        title = stringResource(Res.string.server_dialog_edit_title),
        initialName = server.name,
        initialAddress = server.address,
        initialType = server.type,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ServerDialog(
    title: String,
    initialName: String = "",
    initialAddress: String = "",
    initialType: ServerType = ServerType.PIAWARE,
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, type: ServerType) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var serverType by remember { mutableStateOf(initialType) }

    val isValid = name.isNotBlank() && address.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.server_dialog_name_label)) },
                    isError = name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (name.isBlank()) {
                    Text(
                        text = stringResource(Res.string.server_dialog_name_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(Res.string.server_dialog_address_label)) },
                    isError = address.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (address.isBlank()) {
                    Text(
                        text = stringResource(Res.string.server_dialog_address_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ServerTypeSelector(
                    selectedType = serverType,
                    onTypeSelected = { serverType = it },
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.server_dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(name.trim(), address.trim(), serverType) },
                        enabled = isValid,
                    ) {
                        Text(stringResource(Res.string.server_dialog_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerTypeSelector(
    selectedType: ServerType,
    onTypeSelected: (ServerType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(Res.string.server_dialog_type_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(serverTypeDisplayName(selectedType))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.server_dialog_type_piaware)) },
                    onClick = {
                        onTypeSelected(ServerType.PIAWARE)
                        expanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.server_dialog_type_readsb)) },
                    onClick = {
                        onTypeSelected(ServerType.READSB)
                        expanded = false
                    },
                )
            }
        }

        Text(
            text = serverTypeDescription(selectedType),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun serverTypeDisplayName(type: ServerType): String =
    when (type) {
        ServerType.PIAWARE -> stringResource(Res.string.server_dialog_type_piaware)
        ServerType.READSB -> stringResource(Res.string.server_dialog_type_readsb)
    }

@Composable
private fun serverTypeDescription(type: ServerType): String =
    when (type) {
        ServerType.PIAWARE -> stringResource(Res.string.server_dialog_type_piaware_description)
        ServerType.READSB -> stringResource(Res.string.server_dialog_type_readsb_description)
    }
