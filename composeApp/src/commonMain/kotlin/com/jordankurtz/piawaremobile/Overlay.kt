package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.jordankurtz.piawaremobile.map.MapViewModel
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.openstreetmap_copyright
import piawaremobile.composeapp.generated.resources.planes_count

@Composable
fun Overlay(mapViewModel: MapViewModel, modifier: Modifier) {
    val numberOfPlanes by mapViewModel.numberOfPlanes.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Text(
            // there seems to be a bug with string formatting with stringResource so do it unideally for now
//            text = stringResource(Res.string.planes_count, numberOfPlanes),
            text = "$numberOfPlanes ${stringResource(Res.string.planes_count)}",
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 12.sp)) {
                pushLink(LinkAnnotation.Url("https://www.openstreetmap.org/copyright"))
                append(stringResource(Res.string.openstreetmap_copyright))
                pop()
            }
        }

        Text(
            text = annotatedString,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
        )
    }
}
