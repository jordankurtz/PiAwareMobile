package com.jordankurtz.piawaremobile.map.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TileCacheDebugOverlay(
    stats: TileCacheStats,
    currentZoom: Int,
    zoomSettings: Triple<Int, Int, Int>?,
    modifier: Modifier = Modifier,
) {
    val hitPct = (stats.hitRate * 100).toInt()
    val tilesLine = buildString {
        append("Tiles  D:${stats.diskHits}  O:${stats.offlineHits}  N:${stats.networkFetches}")
        if (stats.errors > 0) append("  E:${stats.errors}")
        append("  $hitPct% cache")
    }
    val zoomLine = buildString {
        append("Zoom $currentZoom")
        if (zoomSettings != null) {
            val (min, max, default) = zoomSettings
            append("  (min:$min  max:$max  default:$default)")
        }
    }
    androidx.compose.foundation.layout.Column(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(text = tilesLine, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = zoomLine, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
