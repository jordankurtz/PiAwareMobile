package com.jordankurtz.piawaremobile.model

import org.jetbrains.compose.resources.DrawableResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_list
import piawaremobile.composeapp.generated.resources.ic_map
import piawaremobile.composeapp.generated.resources.ic_settings

sealed class Screen(val title: String, val icon: DrawableResource) {
    object Map: Screen("Map", Res.drawable.ic_map)
    object List: Screen("List", Res.drawable.ic_list)
    object Settings: Screen("Settings", Res.drawable.ic_settings)
}