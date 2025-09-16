package com.jordankurtz.piawareviewer.model

import org.jetbrains.compose.resources.DrawableResource
import piawareviewer.composeapp.generated.resources.Res
import piawareviewer.composeapp.generated.resources.ic_list
import piawareviewer.composeapp.generated.resources.ic_map
import piawareviewer.composeapp.generated.resources.ic_settings

sealed class Screen(val title: String, val icon: DrawableResource) {
    object Map: Screen("Map", Res.drawable.ic_map)
    object List: Screen("List", Res.drawable.ic_list)
    object Settings: Screen("Settings", Res.drawable.ic_settings)
}