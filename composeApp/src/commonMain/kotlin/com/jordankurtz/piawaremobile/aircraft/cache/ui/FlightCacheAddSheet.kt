package com.jordankurtz.piawaremobile.aircraft.cache.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_cache
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_cancel
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_days_label
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_name_label
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_region_selected
import piawaremobile.composeapp.generated.resources.flight_cache_sheet_select_region

@Composable
fun FlightCacheAddSheet(
    onDismiss: () -> Unit,
    onSelectRegion: () -> Unit,
    onConfirm: (name: String, box: BoundingBox, daysAhead: Int) -> Unit,
    pendingBox: BoundingBox? = null,
) {
    var name by remember { mutableStateOf("") }
    var daysAhead by remember { mutableFloatStateOf(3f) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.flight_cache_sheet_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.flight_cache_sheet_days_label, daysAhead.toInt()))
        Slider(
            value = daysAhead,
            onValueChange = { daysAhead = it },
            valueRange = 1f..14f,
            steps = 12,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSelectRegion, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (pendingBox != null) {
                    stringResource(
                        Res.string.flight_cache_sheet_region_selected,
                        pendingBox.minLat,
                        pendingBox.maxLat,
                        pendingBox.minLon,
                        pendingBox.maxLon,
                    )
                } else {
                    stringResource(Res.string.flight_cache_sheet_select_region)
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { pendingBox?.let { onConfirm(name, it, daysAhead.toInt()) } },
            enabled = name.isNotBlank() && pendingBox != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.flight_cache_sheet_cache))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.flight_cache_sheet_cancel))
        }
    }
}
