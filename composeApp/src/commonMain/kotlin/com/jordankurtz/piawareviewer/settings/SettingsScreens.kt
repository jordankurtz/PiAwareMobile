package com.jordankurtz.piawareviewer.settings

sealed class SettingsScreens {
    object Main: SettingsScreens()
    object Servers: SettingsScreens()
}