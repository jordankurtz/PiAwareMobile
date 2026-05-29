package com.jordankurtz.piawaremobile.extensions

import androidx.compose.ui.graphics.Color
import com.jordankurtz.piawaremobile.map.TileProviderConfig

val TileProviderConfig.overlayColor: Color
    get() = if (isDarkMap) Color.White else Color.Black
