package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.faa_ifr_high
import piawaremobile.composeapp.generated.resources.faa_ifr_high_description
import piawaremobile.composeapp.generated.resources.faa_ifr_low
import piawaremobile.composeapp.generated.resources.faa_ifr_low_description
import piawaremobile.composeapp.generated.resources.overlay_sheet_title
import piawaremobile.composeapp.generated.resources.show_airspace
import piawaremobile.composeapp.generated.resources.show_airspace_description
import piawaremobile.composeapp.generated.resources.show_faa_charts
import piawaremobile.composeapp.generated.resources.show_faa_charts_description
import piawaremobile.composeapp.generated.resources.tfrs
import piawaremobile.composeapp.generated.resources.tfrs_description

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySheet(
    showFaaCharts: Boolean,
    showFaaIfrLow: Boolean,
    showFaaIfrHigh: Boolean,
    showAirspace: Boolean,
    showTfrs: Boolean,
    hasOpenAipKey: Boolean,
    onToggleFaaCharts: () -> Unit,
    onToggleFaaIfrLow: () -> Unit,
    onToggleFaaIfrHigh: () -> Unit,
    onToggleAirspace: () -> Unit,
    onToggleTfrs: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        OverlaySheetContent(
            showFaaCharts = showFaaCharts,
            showFaaIfrLow = showFaaIfrLow,
            showFaaIfrHigh = showFaaIfrHigh,
            showAirspace = showAirspace,
            showTfrs = showTfrs,
            hasOpenAipKey = hasOpenAipKey,
            onToggleFaaCharts = onToggleFaaCharts,
            onToggleFaaIfrLow = onToggleFaaIfrLow,
            onToggleFaaIfrHigh = onToggleFaaIfrHigh,
            onToggleAirspace = onToggleAirspace,
            onToggleTfrs = onToggleTfrs,
        )
    }
}

@Suppress("LongParameterList")
@Composable
fun OverlaySheetContent(
    showFaaCharts: Boolean,
    showFaaIfrLow: Boolean,
    showFaaIfrHigh: Boolean,
    showAirspace: Boolean,
    showTfrs: Boolean,
    hasOpenAipKey: Boolean,
    onToggleFaaCharts: () -> Unit,
    onToggleFaaIfrLow: () -> Unit,
    onToggleFaaIfrHigh: () -> Unit,
    onToggleAirspace: () -> Unit,
    onToggleTfrs: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(Res.string.overlay_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OverlayRow(
            title = stringResource(Res.string.show_faa_charts),
            description = stringResource(Res.string.show_faa_charts_description),
            checked = showFaaCharts,
            onToggle = onToggleFaaCharts,
        )
        HorizontalDivider()
        OverlayRow(
            title = stringResource(Res.string.faa_ifr_low),
            description = stringResource(Res.string.faa_ifr_low_description),
            checked = showFaaIfrLow,
            onToggle = onToggleFaaIfrLow,
        )
        HorizontalDivider()
        OverlayRow(
            title = stringResource(Res.string.faa_ifr_high),
            description = stringResource(Res.string.faa_ifr_high_description),
            checked = showFaaIfrHigh,
            onToggle = onToggleFaaIfrHigh,
        )
        HorizontalDivider()
        OverlayRow(
            title = stringResource(Res.string.show_airspace),
            description = stringResource(Res.string.show_airspace_description),
            checked = showAirspace,
            enabled = hasOpenAipKey,
            onToggle = onToggleAirspace,
        )
        HorizontalDivider()
        OverlayRow(
            title = stringResource(Res.string.tfrs),
            description = stringResource(Res.string.tfrs_description),
            checked = showTfrs,
            onToggle = onToggleTfrs,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OverlayRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) onSurface else onSurface.copy(alpha = 0.38f),
            )
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = 0.38f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled,
        )
    }
}
