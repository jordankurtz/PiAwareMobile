package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.planes_count

@Composable
fun Overlay(
    numberOfPlanes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.planes_count, numberOfPlanes),
        modifier = modifier.padding(4.dp),
        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        color = Color.Black,
    )
}
