package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_chevron_right

@Composable
fun MainScreen(onServersClicked: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsSection(title = "Preferences")
            }


            item {
                SettingsItem(title = "Servers", onClick = onServersClicked, trailingIcon = {
                    Image(
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colors.onBackground
                        )
                    )
                })
            }


            item {
                SettingsNumberInput(
                    title = "Refresh Interval",
                    value = settings.getValue()?.refreshInterval
                        ?: SettingsRepository.DEFAULT_REFRESH_INTERVAL,
                    onValueChange = viewModel::updateRefreshInterval
                )
            }

            item {
                SettingsSwitch(
                    title = "Center map on user",
                    description = "Automatically center the map on your location when the app starts",
                    checked = settings.getValue()?.centerMapOnUserOnStart ?: false,
                    onCheckedChange = viewModel::updateCenterMapOnUserOnStart
                )
            }

            item {
                SettingsSwitch(
                    title = "Restore map position",
                    description = "Save and restore the last map position and zoom level on start",
                    checked = settings.getValue()?.restoreMapStateOnStart ?: true,
                    onCheckedChange = viewModel::updateRestoreMapStateOnStart
                )
            }

        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle2,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )

            trailingIcon?.invoke()
        }
        Divider()
    }
}

@Composable
fun SettingsNumberInput(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf(value.toString()) }
    val isValid = textValue.toIntOrNull() != null

    LaunchedEffect(value) {
        textValue = value.toString()
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.body1)

            BasicTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    it.toIntOrNull()?.let(onValueChange)
                },
                singleLine = true,
                modifier = Modifier
                    .width(60.dp)
                    .padding(start = 8.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textStyle = MaterialTheme.typography.body1.copy(color = if (isValid) Color.Black else Color.Red)
            )
        }
        Divider()
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
            androidx.compose.material.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Divider()
    }
}
