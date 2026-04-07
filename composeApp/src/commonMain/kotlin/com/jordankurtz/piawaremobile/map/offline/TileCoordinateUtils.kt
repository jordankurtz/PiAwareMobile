package com.jordankurtz.piawaremobile.map.offline

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

data class TileCoord(val zoom: Int, val col: Int, val row: Int)

/**
 * Converts a lat/lon coordinate to OSM slippy-map tile col/row at the given zoom.
 * Returns a (col, row) pair clamped to [0, 2^zoom - 1].
 */
fun latLonToTile(
    lat: Double,
    lon: Double,
    zoom: Int,
): Pair<Int, Int> {
    val n = 1 shl zoom // 2^zoom
    val col = floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    val latRad = lat * PI / 180.0
    val row =
        floor(
            (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n,
        ).toInt().coerceIn(0, n - 1)
    return Pair(col, row)
}

/**
 * Returns the total number of tiles covering the given bounding box across all zoom levels
 * from [minZoom] to [maxZoom] inclusive.
 */
fun tileCount(
    bounds: BoundingBox,
    minZoom: Int,
    maxZoom: Int,
): Long {
    var total = 0L
    for (zoom in minZoom..maxZoom) {
        val (colMin, rowMin) = latLonToTile(bounds.maxLat, bounds.minLon, zoom)
        val (colMax, rowMax) = latLonToTile(bounds.minLat, bounds.maxLon, zoom)
        total += (colMax - colMin + 1).toLong() * (rowMax - rowMin + 1).toLong()
    }
    return total
}

/**
 * Yields all [TileCoord]s covering the bounding box across zoom levels [minZoom]..[maxZoom].
 * Uses maxLat/minLon for the NW corner (smallest row/col) and minLat/maxLon for SE.
 */
fun tilesForRegion(
    bounds: BoundingBox,
    minZoom: Int,
    maxZoom: Int,
): Sequence<TileCoord> =
    sequence {
        for (zoom in minZoom..maxZoom) {
            val (colMin, rowMin) = latLonToTile(bounds.maxLat, bounds.minLon, zoom)
            val (colMax, rowMax) = latLonToTile(bounds.minLat, bounds.maxLon, zoom)
            for (col in colMin..colMax) {
                for (row in rowMin..rowMax) {
                    yield(TileCoord(zoom = zoom, col = col, row = row))
                }
            }
        }
    }
