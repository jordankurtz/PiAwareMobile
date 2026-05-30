package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.Alignment
import org.maplibre.compose.map.OrnamentOptions

actual fun defaultOrnamentOptions(): OrnamentOptions = OrnamentOptions(compassAlignment = Alignment.BottomStart)
