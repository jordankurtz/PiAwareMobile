package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jordankurtz.piawaremobile.settings.SettingsScreens

@Composable
fun SettingsScreen() {
    var currentScreen by remember { mutableStateOf<SettingsScreens>(SettingsScreens.Main) }
        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { slideInHorizontally() togetherWith slideOutHorizontally() }
            ) { screen ->
                when (screen) {
                    SettingsScreens.Main -> MainScreen { currentScreen = SettingsScreens.Servers }
                    SettingsScreens.Servers -> ServersScreen() {}
                }
            }
        }
}