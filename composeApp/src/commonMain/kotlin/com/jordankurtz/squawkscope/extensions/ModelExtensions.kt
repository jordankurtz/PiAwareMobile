package com.jordankurtz.squawkscope.extensions

import androidx.compose.ui.graphics.Color
import com.jordankurtz.squawkscope.map.TileProviderConfig

val TileProviderConfig.overlayColor: Color
    get() = if (isDarkMap) Color.White else Color.Black
