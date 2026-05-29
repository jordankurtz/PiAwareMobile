package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jordankurtz.piawaremobile.extensions.overlayColor
import com.jordankurtz.piawaremobile.map.TileProviderConfig
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.planes_count

@Composable
fun Overlay(
    numberOfPlanes: Int,
    provider: TileProviderConfig,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.planes_count, numberOfPlanes),
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = provider.overlayColor,
        )

        val annotatedString =
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 12.sp, color = provider.overlayColor)) {
                    pushLink(LinkAnnotation.Url(provider.copyrightUrl))
                    append(provider.attribution)
                    pop()
                }
            }

        Text(
            text = annotatedString,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        )
    }
}
