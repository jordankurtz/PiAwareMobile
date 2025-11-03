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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.center_map_on_user_description
import piawaremobile.composeapp.generated.resources.center_map_on_user_title
import piawaremobile.composeapp.generated.resources.ic_chevron_right
import piawaremobile.composeapp.generated.resources.open_urls_externally_description
import piawaremobile.composeapp.generated.resources.open_urls_externally_title
import piawaremobile.composeapp.generated.resources.preferences_title
import piawaremobile.composeapp.generated.resources.refresh_interval_title
import piawaremobile.composeapp.generated.resources.restore_map_position_description
import piawaremobile.composeapp.generated.resources.restore_map_position_title
import piawaremobile.composeapp.generated.resources.servers_title
import piawaremobile.composeapp.generated.resources.settings_title
import piawaremobile.composeapp.generated.resources.show_receiver_locations_description
import piawaremobile.composeapp.generated.resources.show_receiver_locations_title
import piawaremobile.composeapp.generated.resources.show_user_location_description
import piawaremobile.composeapp.generated.resources.show_user_location_title
import piawaremobile.composeapp.generated.resources.enable_flightaware_api_title
import piawaremobile.composeapp.generated.resources.enable_flightaware_api_description
import piawaremobile.composeapp.generated.resources.flightaware_api_key_title

@Composable
fun MainScreen(onServersClicked: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsSection(title = stringResource(Res.string.preferences_title))
            }


            item {
                SettingsItem(title = stringResource(Res.string.servers_title), onClick = onServersClicked, trailingIcon = {
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
                    title = stringResource(Res.string.refresh_interval_title),
                    value = settings.getValue()?.refreshInterval
                        ?: SettingsRepository.DEFAULT_REFRESH_INTERVAL,
                    onValueChange = viewModel::updateRefreshInterval
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.center_map_on_user_title),
                    description = stringResource(Res.string.center_map_on_user_description),
                    checked = settings.getValue()?.centerMapOnUserOnStart ?: false,
                    onCheckedChange = viewModel::updateCenterMapOnUserOnStart
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.restore_map_position_title),
                    description = stringResource(Res.string.restore_map_position_description),
                    checked = settings.getValue()?.restoreMapStateOnStart ?: true,
                    onCheckedChange = viewModel::updateRestoreMapStateOnStart
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.show_receiver_locations_title),
                    description = stringResource(Res.string.show_receiver_locations_description),
                    checked = settings.getValue()?.showReceiverLocations ?: true,
                    onCheckedChange = viewModel::updateShowReceiverLocations
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.show_user_location_title),
                    description = stringResource(Res.string.show_user_location_description),
                    checked = settings.getValue()?.showUserLocationOnMap ?: true,
                    onCheckedChange = viewModel::updateShowUserLocationOnMap
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.open_urls_externally_title),
                    description = stringResource(Res.string.open_urls_externally_description),
                    checked = settings.getValue()?.openUrlsExternally ?: false,
                    onCheckedChange = viewModel::updateOpenUrlsExternally
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(Res.string.enable_flightaware_api_title),
                    description = stringResource(Res.string.enable_flightaware_api_description),
                    checked = settings.getValue()?.enableFlightAwareApi ?: false,
                    onCheckedChange = viewModel::updateEnableFlightAwareApi
                )
            }

            item {
                SettingsTextInput(
                    title = stringResource(Res.string.flightaware_api_key_title),
                    value = settings.getValue()?.flightAwareApiKey ?: "",
                    onValueChange = viewModel::updateFlightAwareApiKey
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
fun SettingsTextInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        textValue = value
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
                    onValueChange(it)
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textStyle = MaterialTheme.typography.body1.copy(color = Color.Black)
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
