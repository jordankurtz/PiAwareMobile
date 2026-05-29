package com.jordankurtz.piawaremobile.map

import org.jetbrains.compose.resources.StringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.tile_provider_carto_dark
import piawaremobile.composeapp.generated.resources.tile_provider_carto_dark_nolabels
import piawaremobile.composeapp.generated.resources.tile_provider_carto_light
import piawaremobile.composeapp.generated.resources.tile_provider_carto_light_nolabels
import piawaremobile.composeapp.generated.resources.tile_provider_carto_voyager
import piawaremobile.composeapp.generated.resources.tile_provider_cyclosm
import piawaremobile.composeapp.generated.resources.tile_provider_esri_satellite
import piawaremobile.composeapp.generated.resources.tile_provider_esri_street
import piawaremobile.composeapp.generated.resources.tile_provider_esri_topo
import piawaremobile.composeapp.generated.resources.tile_provider_jawg_dark
import piawaremobile.composeapp.generated.resources.tile_provider_jawg_streets
import piawaremobile.composeapp.generated.resources.tile_provider_openstreetmap
import piawaremobile.composeapp.generated.resources.tile_provider_opentopomap
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_alidade_dark
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_alidade_smooth
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_toner
import piawaremobile.composeapp.generated.resources.tile_provider_stadia_watercolor
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_atlas
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_cycle
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_landscape
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_mobile_atlas
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_neighbourhood
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_outdoors
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_pioneer
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_spinal_map
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_transport
import piawaremobile.composeapp.generated.resources.tile_provider_thunderforest_transport_dark

data class TileProviderConfig(
    val id: String,
    val displayNameRes: StringResource? = null,
    val displayName: String = "",
    val urlTemplate: String,
    val subdomains: List<String> = emptyList(),
    val requestDelayMs: Long = 0L,
    val avgTileSizeBytes: Long = 15_000L,
    val userAgent: String = "PiAwareMobile/1.0 (https://github.com/jordankurtz/PiAwareMobile)",
    val attribution: String = "",
    val copyrightUrl: String,
    val isDarkMap: Boolean = false,
    val requiresApiKey: Boolean = false,
    val apiKeyGroup: String? = null,
) {
    fun buildUrl(
        zoom: Int,
        col: Int,
        row: Int,
        subdomain: String = "",
        apiKey: String = "",
    ): String =
        urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", col.toString())
            .replace("{y}", row.toString())
            .replace("{s}", subdomain)
            .replace("{api_key}", apiKey)
}

object TileProviders {
    val OPENSTREETMAP =
        TileProviderConfig(
            id = "openstreetmap",
            displayNameRes = Res.string.tile_provider_openstreetmap,
            urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            attribution = "© OpenStreetMap contributors",
            copyrightUrl = "https://www.openstreetmap.org/copyright",
        )

    val CARTO_DARK_ALL =
        TileProviderConfig(
            id = "carto_dark_all",
            displayNameRes = Res.string.tile_provider_carto_dark,
            urlTemplate = "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 10_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
            isDarkMap = true,
        )

    val CARTO_DARK_NOLABELS =
        TileProviderConfig(
            id = "carto_dark_nolabels",
            displayNameRes = Res.string.tile_provider_carto_dark_nolabels,
            urlTemplate = "https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 8_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
            isDarkMap = true,
        )

    val CARTO_LIGHT_ALL =
        TileProviderConfig(
            id = "carto_light_all",
            displayNameRes = Res.string.tile_provider_carto_light,
            urlTemplate = "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 12_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val CARTO_LIGHT_NOLABELS =
        TileProviderConfig(
            id = "carto_light_nolabels",
            displayNameRes = Res.string.tile_provider_carto_light_nolabels,
            urlTemplate = "https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 10_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val CARTO_VOYAGER =
        TileProviderConfig(
            id = "carto_voyager",
            displayNameRes = Res.string.tile_provider_carto_voyager,
            urlTemplate = "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 14_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val OPENTOPOMAP =
        TileProviderConfig(
            id = "opentopomap",
            displayNameRes = Res.string.tile_provider_opentopomap,
            urlTemplate = "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c"),
            avgTileSizeBytes = 20_000L,
            attribution = "© OpenTopoMap, © OpenStreetMap contributors",
            copyrightUrl = "https://opentopomap.org/about",
        )

    val CYCLOSM =
        TileProviderConfig(
            id = "cyclosm",
            displayNameRes = Res.string.tile_provider_cyclosm,
            urlTemplate = "https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c"),
            avgTileSizeBytes = 15_000L,
            attribution = "© CyclOSM, © OpenStreetMap contributors",
            copyrightUrl = "https://www.cyclosm.org/#map=12/48.8566/2.3522/cyclosm",
        )

    val ESRI_SATELLITE =
        TileProviderConfig(
            id = "esri_satellite",
            displayNameRes = Res.string.tile_provider_esri_satellite,
            urlTemplate =
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            avgTileSizeBytes = 30_000L,
            attribution = "Tiles © Esri",
            copyrightUrl = "https://www.esri.com/",
            isDarkMap = true,
        )

    val ESRI_TOPO =
        TileProviderConfig(
            id = "esri_topo",
            displayNameRes = Res.string.tile_provider_esri_topo,
            urlTemplate =
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}",
            avgTileSizeBytes = 25_000L,
            attribution = "Tiles © Esri",
            copyrightUrl = "https://www.esri.com/",
        )

    val ESRI_STREET =
        TileProviderConfig(
            id = "esri_street",
            displayNameRes = Res.string.tile_provider_esri_street,
            urlTemplate =
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}",
            avgTileSizeBytes = 20_000L,
            attribution = "Tiles © Esri",
            copyrightUrl = "https://www.esri.com/",
        )

    // API-key providers
    val STADIA_TONER =
        TileProviderConfig(
            id = "stadia_toner",
            displayNameRes = Res.string.tile_provider_stadia_toner,
            urlTemplate = "https://tiles.stadiamaps.com/tiles/stamen_toner/{z}/{x}/{y}.png?api_key={api_key}",
            attribution = "© Stadia Maps, © Stamen Design, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com/",
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val STADIA_WATERCOLOR =
        TileProviderConfig(
            id = "stadia_watercolor",
            displayNameRes = Res.string.tile_provider_stadia_watercolor,
            urlTemplate = "https://tiles.stadiamaps.com/tiles/stamen_watercolor/{z}/{x}/{y}.jpg?api_key={api_key}",
            attribution = "© Stadia Maps, © Stamen Design, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com/",
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val STADIA_ALIDADE_SMOOTH =
        TileProviderConfig(
            id = "stadia_alidade_smooth",
            displayNameRes = Res.string.tile_provider_stadia_alidade_smooth,
            urlTemplate = "https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}.png?api_key={api_key}",
            attribution = "© Stadia Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com/",
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val STADIA_ALIDADE_DARK =
        TileProviderConfig(
            id = "stadia_alidade_dark",
            displayNameRes = Res.string.tile_provider_stadia_alidade_dark,
            urlTemplate = "https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}.png?api_key={api_key}",
            attribution = "© Stadia Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://stadiamaps.com/",
            isDarkMap = true,
            requiresApiKey = true,
            apiKeyGroup = "stadia",
        )

    val THUNDERFOREST_TRANSPORT =
        TileProviderConfig(
            id = "thunderforest_transport",
            displayNameRes = Res.string.tile_provider_thunderforest_transport,
            urlTemplate = "https://tile.thunderforest.com/transport/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_CYCLE =
        TileProviderConfig(
            id = "thunderforest_cycle",
            displayNameRes = Res.string.tile_provider_thunderforest_cycle,
            urlTemplate = "https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_OUTDOORS =
        TileProviderConfig(
            id = "thunderforest_outdoors",
            displayNameRes = Res.string.tile_provider_thunderforest_outdoors,
            urlTemplate = "https://tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_LANDSCAPE =
        TileProviderConfig(
            id = "thunderforest_landscape",
            displayNameRes = Res.string.tile_provider_thunderforest_landscape,
            urlTemplate = "https://tile.thunderforest.com/landscape/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_TRANSPORT_DARK =
        TileProviderConfig(
            id = "thunderforest_transport_dark",
            displayNameRes = Res.string.tile_provider_thunderforest_transport_dark,
            urlTemplate = "https://tile.thunderforest.com/transport-dark/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            isDarkMap = true,
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_SPINAL_MAP =
        TileProviderConfig(
            id = "thunderforest_spinal_map",
            displayNameRes = Res.string.tile_provider_thunderforest_spinal_map,
            urlTemplate = "https://tile.thunderforest.com/spinal-map/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            isDarkMap = true,
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_PIONEER =
        TileProviderConfig(
            id = "thunderforest_pioneer",
            displayNameRes = Res.string.tile_provider_thunderforest_pioneer,
            urlTemplate = "https://tile.thunderforest.com/pioneer/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_MOBILE_ATLAS =
        TileProviderConfig(
            id = "thunderforest_mobile_atlas",
            displayNameRes = Res.string.tile_provider_thunderforest_mobile_atlas,
            urlTemplate = "https://tile.thunderforest.com/mobile-atlas/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_NEIGHBOURHOOD =
        TileProviderConfig(
            id = "thunderforest_neighbourhood",
            displayNameRes = Res.string.tile_provider_thunderforest_neighbourhood,
            urlTemplate = "https://tile.thunderforest.com/neighbourhood/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val THUNDERFOREST_ATLAS =
        TileProviderConfig(
            id = "thunderforest_atlas",
            displayNameRes = Res.string.tile_provider_thunderforest_atlas,
            urlTemplate = "https://tile.thunderforest.com/atlas/{z}/{x}/{y}.png?apikey={api_key}",
            attribution = "© Thunderforest, © OpenStreetMap contributors",
            copyrightUrl = "https://www.thunderforest.com/",
            requiresApiKey = true,
            apiKeyGroup = "thunderforest",
        )

    val JAWG_STREETS =
        TileProviderConfig(
            id = "jawg_streets",
            displayNameRes = Res.string.tile_provider_jawg_streets,
            urlTemplate = "https://tile.jawg.io/jawg-streets/{z}/{x}/{y}.png?access-token={api_key}",
            attribution = "© Jawg Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://www.jawg.io/",
            requiresApiKey = true,
            apiKeyGroup = "jawg",
        )

    val JAWG_DARK =
        TileProviderConfig(
            id = "jawg_dark",
            displayNameRes = Res.string.tile_provider_jawg_dark,
            urlTemplate = "https://tile.jawg.io/jawg-dark/{z}/{x}/{y}.png?access-token={api_key}",
            attribution = "© Jawg Maps, © OpenStreetMap contributors",
            copyrightUrl = "https://www.jawg.io/",
            isDarkMap = true,
            requiresApiKey = true,
            apiKeyGroup = "jawg",
        )

    val BUILT_IN: List<TileProviderConfig> =
        listOf(
            OPENSTREETMAP,
            CARTO_VOYAGER,
            CARTO_DARK_ALL,
            CARTO_DARK_NOLABELS,
            CARTO_LIGHT_ALL,
            CARTO_LIGHT_NOLABELS,
            OPENTOPOMAP,
            CYCLOSM,
            ESRI_SATELLITE,
            ESRI_TOPO,
            ESRI_STREET,
        )

    val API_KEY_REQUIRED: List<TileProviderConfig> =
        listOf(
            STADIA_TONER,
            STADIA_WATERCOLOR,
            STADIA_ALIDADE_SMOOTH,
            STADIA_ALIDADE_DARK,
            THUNDERFOREST_TRANSPORT,
            THUNDERFOREST_TRANSPORT_DARK,
            THUNDERFOREST_CYCLE,
            THUNDERFOREST_OUTDOORS,
            THUNDERFOREST_LANDSCAPE,
            THUNDERFOREST_PIONEER,
            THUNDERFOREST_ATLAS,
            THUNDERFOREST_NEIGHBOURHOOD,
            THUNDERFOREST_MOBILE_ATLAS,
            THUNDERFOREST_SPINAL_MAP,
            JAWG_STREETS,
            JAWG_DARK,
        )

    val ALL: List<TileProviderConfig> = BUILT_IN + API_KEY_REQUIRED

    fun findById(id: String): TileProviderConfig = ALL.find { it.id == id } ?: OPENSTREETMAP
}
