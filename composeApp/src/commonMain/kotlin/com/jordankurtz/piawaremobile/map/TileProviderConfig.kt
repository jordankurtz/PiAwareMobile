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
import piawaremobile.composeapp.generated.resources.tile_provider_openstreetmap
import piawaremobile.composeapp.generated.resources.tile_provider_opentopomap

data class TileProviderConfig(
    val id: String,
    val displayNameRes: StringResource,
    val urlTemplate: String,
    val subdomains: List<String> = emptyList(),
    val requestDelayMs: Long = 0L,
    val avgTileSizeBytes: Long = 15_000L,
    val userAgent: String = "PiAwareMobile/1.0 (https://github.com/jordankurtz/PiAwareMobile)",
    val attribution: String = "",
    val copyrightUrl: String,
    val isDarkMap: Boolean = false,
) {
    fun buildUrl(
        zoom: Int,
        col: Int,
        row: Int,
        subdomain: String = "",
    ): String =
        urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", col.toString())
            .replace("{y}", row.toString())
            .replace("{s}", subdomain)
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

    val ALL: List<TileProviderConfig> =
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

    fun findById(id: String): TileProviderConfig = ALL.find { it.id == id } ?: OPENSTREETMAP
}
