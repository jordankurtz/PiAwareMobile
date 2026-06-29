package com.jordankurtz.piawaremobile.squawk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.squawk.SquawkCodes
import com.jordankurtz.piawaremobile.squawk.SquawkSeverity
import com.jordankurtz.piawaremobile.ui.AppTheme

@Composable
fun SquawkInfoDialog(
    squawk: String,
    onDismiss: () -> Unit,
) {
    val info = SquawkCodes[squawk]
    val name = info?.name ?: "Unknown Code"
    val description = info?.description ?: "No specific meaning is assigned to this squawk code."
    val severityChip: Pair<Color, String>? =
        info?.severity?.let { s ->
            severityColor(s)?.let { c -> c to s.label }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(squawk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                severityChip?.let { (color, label) ->
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
}

@Composable
private fun severityColor(severity: SquawkSeverity): Color? =
    when (severity) {
        SquawkSeverity.EMERGENCY -> AppTheme.colors.aircraftEmergency
        SquawkSeverity.CAUTION -> AppTheme.colors.caution
        SquawkSeverity.INFO -> MaterialTheme.colorScheme.secondary
        SquawkSeverity.NORMAL -> null
    }
