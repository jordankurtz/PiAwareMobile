package com.jordankurtz.piawaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.model.Screen
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }
    Theme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(currentScreen = currentScreen) {
                    currentScreen = it
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Map -> MapScreen()
                    Screen.List -> Box {}
                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val items = listOf(Screen.Map, Screen.List, Screen.Settings)

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(screen.icon),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(screen.title)) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) }
            )
        }
    }
}
