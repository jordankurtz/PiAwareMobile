package com.jordankurtz.squawkscope.settings

sealed class SettingsScreens {
    object Main : SettingsScreens()

    object Servers : SettingsScreens()

    object OfflineMaps : SettingsScreens()

    object MapProviders : SettingsScreens()
}
