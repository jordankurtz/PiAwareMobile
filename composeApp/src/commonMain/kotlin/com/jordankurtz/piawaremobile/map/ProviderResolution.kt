package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import com.jordankurtz.piawaremobile.settings.Settings

fun resolveActiveProviderConfig(
    settings: Settings,
    builtIns: List<TileProviderConfig> = TileProviders.ALL,
): TileProviderConfig {
    val allProviders = builtIns + settings.customProviders.map { it.toTileProviderConfig() }
    val config = allProviders.find { it.id == settings.mapProviderId } ?: TileProviders.DEFAULT
    return if (config.requiresApiKey) {
        val key = settings.apiKeys[config.apiKeyGroup ?: config.id]?.takeIf { it.isNotBlank() }
            ?: return TileProviders.DEFAULT
        config.copy(styleUrl = config.resolvedStyleUrl(key))
    } else {
        config
    }
}

fun CustomProviderConfig.toTileProviderConfig() =
    TileProviderConfig(
        id = id,
        displayName = displayName,
        styleUrl = styleUrl,
        isBuiltIn = false,
    )
