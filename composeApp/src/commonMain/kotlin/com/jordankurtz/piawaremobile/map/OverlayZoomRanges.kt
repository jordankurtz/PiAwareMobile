package com.jordankurtz.piawaremobile.map

/**
 * Zoom range constants for aeronautical overlays.
 * Each range defines the min/max zoom levels at which MapLibre tiles are available.
 * Beyond these ranges, MapLibre overzooms (displays the last available tile scaled).
 */
object OverlayZoomRanges {
    /** FAA VFR Sectional tiles available at zoom 8–12 (~1:2.3M to ~1:144K) */
    val FAA_SECTIONAL: IntRange = 8..12

    /** FAA IFR Area Low tiles available at zoom 7–12 */
    val IFR_LOW: IntRange = 7..12

    /** FAA IFR High tiles available at zoom 5–9 */
    val IFR_HIGH: IntRange = 5..9

    /** OpenAIP Airspace tiles available at zoom 7–14 */
    val AIRSPACE: IntRange = 7..14
}
