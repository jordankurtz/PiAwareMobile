package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.settings.Server

@Composable
fun ServerList(servers: List<Server>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(servers) { server ->
            ServerItem(server)
            Divider()
        }
    }
}

@Composable
fun ServerItem(server: Server, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = server.name, style = MaterialTheme.typography.subtitle1)
        Text(text = server.address, style = MaterialTheme.typography.body2)
    }
}