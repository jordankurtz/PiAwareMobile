package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.graphics.Color

const val GROUND_ALTITUDE = "ground"

fun getColorForAltitude(altitude: String?): Color {
    if (altitude == GROUND_ALTITUDE) {
        return Color(139, 69, 19)
    }
    return when (altitude?.toIntOrNull() ?: 0) {
        in 0..250 -> Color(255, 64, 0)
        in 251..500 -> Color(255, 128, 0)
        in 501..750 -> Color(255, 160, 0)
        in 751..1000 -> Color(255, 192, 0)
        in 1001..1500 -> Color(255, 224, 0)
        in 1501..2000 -> Color(255, 255, 0)
        in 2001..3000 -> Color(192, 255, 0)
        in 3001..4000 -> Color(128, 255, 0)
        in 4001..5000 -> Color(64, 255, 0)
        in 5001..6000 -> Color(0, 255, 64)
        in 6001..7000 -> Color(0, 255, 128)
        in 7001..8000 -> Color(0, 255, 192)
        in 8001..9000 -> Color(0, 255, 224)
        in 9001..10000 -> Color(0, 255, 255)
        in 10001..15000 -> Color(0, 224, 255)
        in 15001..20000 -> Color(0, 192, 255)
        in 20001..25000 -> Color(0, 160, 255)
        in 25001..30000 -> Color(0, 128, 255)
        in 30001..35000 -> Color(0, 64, 255)
        in 35001..40000 -> Color(0, 0, 255)
        in 40001..45000 -> Color(64, 0, 255)
        in 45001..50000 -> Color(128, 0, 255)
        else -> Color(192, 0, 255)
    }
}

/**
 * Result of computing the fit-to-aircraft target.
 */
sealed class FitTarget {
    data class SinglePoint(val latitude: Double, val longitude: Double) : FitTarget()

    data class BoundingRegion(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double,
    ) : FitTarget()
}

/**
 * Given a list of (lat, lon) pairs, compute the fit target:
 * - null if the list is empty
 * - SinglePoint if there is exactly one coordinate
 * - BoundingRegion for two or more coordinates
 */
fun computeFitTarget(coordinates: List<Pair<Double, Double>>): FitTarget? {
    if (coordinates.isEmpty()) return null
    if (coordinates.size == 1) {
        return FitTarget.SinglePoint(coordinates.first().first, coordinates.first().second)
    }
    return FitTarget.BoundingRegion(
        north = coordinates.maxOf { it.first },
        south = coordinates.minOf { it.first },
        east = coordinates.maxOf { it.second },
        west = coordinates.minOf { it.second },
    )
}
