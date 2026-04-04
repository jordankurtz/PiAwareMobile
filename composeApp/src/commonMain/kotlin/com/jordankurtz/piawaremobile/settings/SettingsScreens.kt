package com.jordankurtz.piawaremobile.settings

sealed class SettingsScreens {
    object Main : SettingsScreens()

    object Servers : SettingsScreens()

    object OfflineMaps : SettingsScreens()
}
