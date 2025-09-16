package com.jordankurtz.piawaremobile

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jordankurtz.piawaremobile.map.MapViewModel

@Composable
fun Overlay(mapViewModel: MapViewModel, modifier: Modifier) {
    val numberOfPlanes by mapViewModel.numberOfPlanes.collectAsState()
    Text(
        text = "$numberOfPlanes planes",
        modifier = modifier,
        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    )
}