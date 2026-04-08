package com.jordankurtz.piawaremobile.map

data class TileProviderConfig(
    val id: String,
    val displayName: String,
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
            displayName = "OpenStreetMap",
            urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            attribution = "© OpenStreetMap contributors",
            copyrightUrl = "https://www.openstreetmap.org/copyright",
        )

    val CARTO_DARK_ALL =
        TileProviderConfig(
            id = "carto_dark_all",
            displayName = "CARTO Dark",
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
            displayName = "CARTO Dark (No Labels)",
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
            displayName = "CARTO Light",
            urlTemplate = "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 12_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val CARTO_LIGHT_NOLABELS =
        TileProviderConfig(
            id = "carto_light_nolabels",
            displayName = "CARTO Light (No Labels)",
            urlTemplate = "https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 10_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val CARTO_VOYAGER =
        TileProviderConfig(
            id = "carto_voyager",
            displayName = "CARTO Voyager",
            urlTemplate = "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png",
            subdomains = listOf("a", "b", "c", "d"),
            avgTileSizeBytes = 14_000L,
            attribution = "© CARTO, © OpenStreetMap contributors",
            copyrightUrl = "https://carto.com/legal/",
        )

    val ESRI_SATELLITE =
        TileProviderConfig(
            id = "esri_satellite",
            displayName = "ESRI Satellite",
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
            displayName = "ESRI Topographic",
            urlTemplate =
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}",
            avgTileSizeBytes = 25_000L,
            attribution = "Tiles © Esri",
            copyrightUrl = "https://www.esri.com/",
        )

    val ESRI_STREET =
        TileProviderConfig(
            id = "esri_street",
            displayName = "ESRI Street",
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
            ESRI_SATELLITE,
            ESRI_TOPO,
            ESRI_STREET,
        )

    fun findById(id: String): TileProviderConfig = ALL.find { it.id == id } ?: OPENSTREETMAP
}
