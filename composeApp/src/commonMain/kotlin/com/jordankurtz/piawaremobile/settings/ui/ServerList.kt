package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.settings.Server

@Composable
fun ServerList(servers: List<Server>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(servers) { server ->
            ServerItem(server)
            HorizontalDivider()
        }
    }
}

@Composable
fun ServerItem(server: Server, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = server.name, style = MaterialTheme.typography.titleMedium)
        Text(text = server.address, style = MaterialTheme.typography.bodyMedium)
    }
}
