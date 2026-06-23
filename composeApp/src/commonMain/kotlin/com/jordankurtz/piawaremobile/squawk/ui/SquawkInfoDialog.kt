package com.jordankurtz.piawaremobile.squawk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val chipColor = info?.severity?.let { severityColor(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(squawk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                chipColor?.let { color ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(info!!.severity.label) },
                        colors =
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = color.copy(alpha = 0.15f),
                                labelColor = color,
                            ),
                    )
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
