package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jordankurtz.piawaremobile.getPlatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.navigate_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val isAndroid = remember { getPlatform().name.startsWith("Android") }
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.navigate_back),
                )
            }
        }
    }
    if (isAndroid) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = navigationIcon,
            actions = actions,
        )
    } else {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}
