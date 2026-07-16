package com.jordankurtz.squawkscope.model

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import squawkscope.composeapp.generated.resources.Res
import squawkscope.composeapp.generated.resources.ic_list
import squawkscope.composeapp.generated.resources.ic_map
import squawkscope.composeapp.generated.resources.ic_settings
import squawkscope.composeapp.generated.resources.screen_list
import squawkscope.composeapp.generated.resources.screen_map
import squawkscope.composeapp.generated.resources.screen_settings

sealed class Screen(
    val title: StringResource,
    val icon: DrawableResource,
) {
    object Map : Screen(Res.string.screen_map, Res.drawable.ic_map)

    object List : Screen(Res.string.screen_list, Res.drawable.ic_list)

    object Settings : Screen(Res.string.screen_settings, Res.drawable.ic_settings)
}
