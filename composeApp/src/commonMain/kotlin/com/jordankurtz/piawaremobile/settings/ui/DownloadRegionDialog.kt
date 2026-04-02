package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_bounds_label
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_cancel
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_download
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_estimate
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_max_zoom
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_min_zoom
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_name_label
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_select_on_map
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_title
import kotlin.math.abs
import kotlin.math.round

private const val DEFAULT_MIN_ZOOM = 8f
private const val DEFAULT_MAX_ZOOM = 14f
private const val MIN_ZOOM_LIMIT = 1f
private const val MAX_ZOOM_LIMIT = 18f
private const val COORD_DECIMAL_PLACES = 4
private const val COORD_SCALE = 10_000.0

private fun formatCoord(value: Double): String {
    val rounded = round(abs(value) * COORD_SCALE) / COORD_SCALE
    val sign = if (value < 0.0) "-" else ""
    val intPart = rounded.toLong()
    val fracPart = round((rounded - intPart) * COORD_SCALE).toLong()
    return "$sign$intPart.${fracPart.toString().padStart(COORD_DECIMAL_PLACES, '0')}"
}

@Composable
fun DownloadRegionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, minZoom: Int, maxZoom: Int) -> Unit,
    selectedBounds: BoundingBox? = null,
    onSelectOnMap: () -> Unit = {},
) {
    var name by remember { mutableStateOf("") }
    var minZoom by remember { mutableFloatStateOf(DEFAULT_MIN_ZOOM) }
    var maxZoom by remember { mutableFloatStateOf(DEFAULT_MAX_ZOOM) }

    val isValid = name.isNotBlank()

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
                Text(
                    text = stringResource(Res.string.offline_maps_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.offline_maps_dialog_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSelectOnMap,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.offline_maps_dialog_select_on_map))
                }

                if (selectedBounds != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            stringResource(
                                Res.string.offline_maps_dialog_bounds_label,
                                formatCoord(selectedBounds.minLat),
                                formatCoord(selectedBounds.minLon),
                                formatCoord(selectedBounds.maxLat),
                                formatCoord(selectedBounds.maxLon),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.offline_maps_dialog_min_zoom, minZoom.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = minZoom,
                    onValueChange = {
                        minZoom = it
                        if (maxZoom < it) {
                            maxZoom = it
                        }
                    },
                    valueRange = MIN_ZOOM_LIMIT..MAX_ZOOM_LIMIT,
                    steps = (MAX_ZOOM_LIMIT - MIN_ZOOM_LIMIT).toInt() - 1,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.offline_maps_dialog_max_zoom, maxZoom.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = maxZoom,
                    onValueChange = {
                        maxZoom = it
                        if (minZoom > it) {
                            minZoom = it
                        }
                    },
                    valueRange = MIN_ZOOM_LIMIT..MAX_ZOOM_LIMIT,
                    steps = (MAX_ZOOM_LIMIT - MIN_ZOOM_LIMIT).toInt() - 1,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.offline_maps_dialog_estimate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.offline_maps_dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(name.trim(), minZoom.toInt(), maxZoom.toInt()) },
                        enabled = isValid,
                    ) {
                        Text(stringResource(Res.string.offline_maps_dialog_download))
                    }
                }
            }
        }
    }
}
