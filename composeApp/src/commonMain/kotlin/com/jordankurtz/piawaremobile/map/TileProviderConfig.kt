package com.jordankurtz.piawaremobile.map

import org.jetbrains.compose.resources.StringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.tile_provider_maptiler_outdoor
import piawaremobile.composeapp.generated.resources.tile_provider_maptiler_streets
import piawaremobile.composeapp.generated.resources.tile_provider_openfreemap_bright
import piawaremobile.composeapp.generated.resources.tile_provider_openfreemap_positron
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_alidade_smooth
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_outdoors

data class TileProviderConfig(
    val id: String,
    val displayNameRes: StringResource? = null,
    val displayName: String = "",
    val styleUrl: String,
    val attribution: String = "",
    val copyrightUrl: String = "",
    val requiresApiKey: Boolean = false,
    val apiKeyGroup: String? = null,
    val isBuiltIn: Boolean = true,
) {
    fun resolvedStyleUrl(apiKey: String): String = styleUrl.replace("{api_key}", apiKey)
}

object TileProviders {
    val OPENFREEMAP_BRIGHT =
        TileProviderConfig(
            id = "openfreemap-bright",
            displayNameRes = Res.string.tile_provider_openfreemap_bright,
            styleUrl = "https://tiles.openfreemap.org/styles/bright",
            attribution = "© OpenFreeMap, © OpenStreetMap contributors",
            copyrightUrl = "https://openfreemap.org",
        )

    val OPENFREEMAP_POSITRON =
        TileProviderConfig(
            id = "openfreemap-positron",
            displayNameRes = Res.string.tile_provider_openfreemap_positron,
            styleUrl = "https://tiles.openfreemap.org/styles/positron",
            attribution = "© OpenFreeMap, © OpenStreetMap contributors",
            copyrightUrl = "https://openfreemap.org",
        )

    val STADIA_ALIDADE_SMOOTH =
        TileProviderConfig(
            id = "stadia-alidade-smooth",
            displayNameRes = Res.string.tile_provider_stadia_alidade_smooth,
            styleUrl = "https://tiles.stadiamaps.com/styles/alidade_smooth.json?api_key={api_key}",
            attribution = "© Stadia Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com",
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val STADIA_OUTDOORS =
        TileProviderConfig(
            id = "stadia-outdoors",
            displayNameRes = Res.string.tile_provider_stadia_outdoors,
            styleUrl = "https://tiles.stadiamaps.com/styles/outdoors.json?api_key={api_key}",
            attribution = "© Stadia Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com",
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val MAPTILER_STREETS =
        TileProviderConfig(
            id = "maptiler-streets",
            displayNameRes = Res.string.tile_provider_maptiler_streets,
            styleUrl = "https://api.maptiler.com/maps/streets/style.json?key={api_key}",
            attribution = "© MapTiler, © OpenStreetMap contributors",
            copyrightUrl = "https://maptiler.com",
            requiresApiKey = true,
            apiKeyGroup = "maptiler",
        )

    val MAPTILER_OUTDOOR =
        TileProviderConfig(
            id = "maptiler-outdoor",
            displayNameRes = Res.string.tile_provider_maptiler_outdoor,
            styleUrl = "https://api.maptiler.com/maps/outdoor/style.json?key={api_key}",
            attribution = "© MapTiler, © OpenStreetMap contributors",
            copyrightUrl = "https://maptiler.com",
            requiresApiKey = true,
            apiKeyGroup = "maptiler",
        )

    val ALL: List<TileProviderConfig> =
        listOf(
            OPENFREEMAP_BRIGHT,
            OPENFREEMAP_POSITRON,
            STADIA_ALIDADE_SMOOTH,
            STADIA_OUTDOORS,
            MAPTILER_STREETS,
            MAPTILER_OUTDOOR,
        )

    val DEFAULT = OPENFREEMAP_BRIGHT
}
