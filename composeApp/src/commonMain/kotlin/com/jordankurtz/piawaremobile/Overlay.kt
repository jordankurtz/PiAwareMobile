package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jordankurtz.piawaremobile.extensions.overlayColor
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.planes_count

@Composable
fun Overlay(
    numberOfPlanes: Int,
    provider: TileProviderConfig,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_plane),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.planes_count, numberOfPlanes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        val annotatedString =
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 12.sp, color = provider.overlayColor)) {
                    append(provider.attribution)
                }
            }

        Text(
            text = annotatedString,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        )
    }
}
