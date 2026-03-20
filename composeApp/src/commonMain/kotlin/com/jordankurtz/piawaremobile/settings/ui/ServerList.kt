package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.settings.Server
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_delete
import piawaremobile.composeapp.generated.resources.ic_edit
import piawaremobile.composeapp.generated.resources.server_delete
import piawaremobile.composeapp.generated.resources.server_edit

@Composable
fun ServerList(
    servers: List<Server>,
    onEditServer: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(servers, key = { it.id }) { server ->
            ServerItem(
                server = server,
                onEdit = { onEditServer(server) },
                onDelete = { onDeleteServer(server) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun ServerItem(
    server: Server,
    onEdit: () -> Unit,
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
            Text(text = server.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = server.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = stringResource(Res.string.server_edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.server_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
