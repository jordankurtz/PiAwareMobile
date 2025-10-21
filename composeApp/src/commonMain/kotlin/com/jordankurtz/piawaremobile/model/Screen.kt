package com.jordankurtz.piawaremobile.model

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_list
import piawaremobile.composeapp.generated.resources.ic_map
import piawaremobile.composeapp.generated.resources.ic_settings
import piawaremobile.composeapp.generated.resources.screen_list
import piawaremobile.composeapp.generated.resources.screen_map
import piawaremobile.composeapp.generated.resources.screen_settings

sealed class Screen(val title: StringResource, val icon: DrawableResource) {
    object Map: Screen(Res.string.screen_map, Res.drawable.ic_map)
    object List: Screen(Res.string.screen_list, Res.drawable.ic_list)
    object Settings: Screen(Res.string.screen_settings, Res.drawable.ic_settings)
}
