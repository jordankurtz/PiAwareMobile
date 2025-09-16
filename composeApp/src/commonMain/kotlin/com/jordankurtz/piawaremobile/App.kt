package com.jordankurtz.piawaremobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.model.Screen
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.jetbrains.compose.resources.painterResource
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
        ) {
            when (currentScreen) {
                Screen.Map -> MapScreen()
                Screen.List -> Box {}
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val items = listOf(Screen.Map, Screen.List, Screen.Settings)

    BottomNavigation {
        items.forEach { screen ->
            BottomNavigationItem(
                icon = {
                    Image(
                        painter = painterResource(screen.icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colors.onPrimary
                        )
                    )
                },
                label = { Text(screen.title) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) }
            )
        }
    }
}

