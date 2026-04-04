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
    modifier: Modifier = Modifier,
) {
    val hitPct = (stats.hitRate * 100).toInt()
    val label =
        buildString {
            append("Tiles  D:${stats.diskHits}  N:${stats.networkFetches}")
            if (stats.errors > 0) append("  E:${stats.errors}")
            append("  $hitPct% cache")
        }
    Text(
        text = label,
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
    )
}
