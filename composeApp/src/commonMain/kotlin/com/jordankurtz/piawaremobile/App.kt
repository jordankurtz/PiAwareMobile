package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jordankurtz.piawaremobile.list.AircraftListScreen
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.model.Screen
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.LocalWindowSize
import com.jordankurtz.piawaremobile.ui.MapWithListLayout
import com.jordankurtz.piawaremobile.ui.Theme
import com.jordankurtz.piawaremobile.ui.WindowSize
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.back_to_list
import piawaremobile.composeapp.generated.resources.ic_arrow_back
import piawaremobile.composeapp.generated.resources.settings_title

@Composable
@Preview
fun App() {
    Theme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowSize = WindowSize(maxWidth, maxHeight)

            CompositionLocalProvider(LocalWindowSize provides windowSize) {
                if (windowSize.isTablet) {
                    ExpandedLayout()
                } else {
                    CompactLayout()
                }
            }
        }
    }
}

/**
 * Phone layout with bottom navigation bar and separate screens.
 */
@Composable
private fun CompactLayout() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentScreen = currentScreen) {
                currentScreen = it
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Map -> MapScreen()
                Screen.List -> AircraftListScreen()
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

/**
 * Tablet layout with map+list side by side and settings icon overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedLayout() {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { showSettings = false }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.back_to_list),
                        )
                    }
                },
            )
            SettingsScreen()
        }
    } else {
        MapWithListLayout(onSettingsClick = { showSettings = true })
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    val items = listOf(Screen.Map, Screen.List, Screen.Settings)

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(screen.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(screen.title)) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
            )
        }
    }
}
