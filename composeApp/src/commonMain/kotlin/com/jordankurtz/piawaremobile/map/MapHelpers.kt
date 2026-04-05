package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.graphics.Color
import com.jordankurtz.piawaremobile.model.Location
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

const val MAX_LEVEL = 16
const val MIN_LEVEL = 1

const val TILE_SIZE = 256
private const val X0 = -2.0037508342789248E7
val mapSize = mapSizeAtLevel(MAX_LEVEL, tileSize = TILE_SIZE)

const val GROUND_ALTITUDE = "ground"

fun mapSizeAtLevel(
    wmtsLevel: Int,
    tileSize: Int,
): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

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
    data class SinglePoint(val x: Double, val y: Double) : FitTarget()

    data class BoundingRegion(
        val xLeft: Double,
        val yTop: Double,
        val xRight: Double,
        val yBottom: Double,
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

    val projected = coordinates.map { (lat, lon) -> doProjection(lat, lon) }

    if (projected.size == 1) {
        return FitTarget.SinglePoint(projected.first().first, projected.first().second)
    }

    return FitTarget.BoundingRegion(
        xLeft = projected.minOf { it.first },
        yTop = projected.minOf { it.second },
        xRight = projected.maxOf { it.first },
        yBottom = projected.maxOf { it.second },
    )
}

val Location.projected: Pair<Double, Double>
    get() = doProjection(latitude, longitude)

fun doProjection(
    latitude: Double?,
    longitude: Double?,
): Pair<Double, Double> {
    if (latitude == null || longitude == null || abs(latitude) > 90 || abs(longitude) > 180) {
        error("Invalid latitude or longitude")
    }

    val num = longitude * 0.017453292519943295 // 2*pi / 360
    val a = latitude * 0.017453292519943295

    val x = normalize(6378137.0 * num, min = X0, max = -X0)
    val y = normalize(3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a))), min = -X0, max = X0)

    return Pair(x, y)
}

private fun normalize(
    t: Double,
    min: Double,
    max: Double,
): Double {
    return (t - min) / (max - min)
}

/**
 * Inverse of [doProjection]: converts normalized [0,1] map coordinates back to geographic degrees.
 *
 * @param normX Normalized x coordinate in [0,1] (increases eastward)
 * @param normY Normalized y coordinate in [0,1] (increases southward, 0 = north pole)
 * @return Pair of (latitude, longitude) in degrees
 */
fun invertProjection(normX: Double, normY: Double): Pair<Double, Double> {
    val halfRange = -X0 // 20037508.342789248
    val xMerc = normX * (2.0 * halfRange) - halfRange
    val lon = xMerc / 6378137.0 * (180.0 / PI)

    val yMerc = halfRange * (1.0 - 2.0 * normY)
    val u = yMerc / 3189068.5
    val sinLat = (exp(u) - 1.0) / (exp(u) + 1.0)
    val lat = asin(sinLat) * (180.0 / PI)

    return Pair(lat, lon)
}

/**
 * Converts a screen pixel position to geographic (lat, lon) degrees using the current viewport.
 *
 * @param scrollX Normalized [0,1] x-coordinate of the viewport center (MapState.scroll.x)
 * @param scrollY Normalized [0,1] y-coordinate of the viewport center (MapState.scroll.y)
 * @param scale Current map scale factor (MapState.scale)
 * @return Pair of (latitude, longitude) in degrees
 */
fun screenToLatLon(
    screenX: Float,
    screenY: Float,
    screenWidth: Float,
    screenHeight: Float,
    scrollX: Double,
    scrollY: Double,
    scale: Float,
): Pair<Double, Double> {
    val normX = scrollX + (screenX - screenWidth / 2f) / (mapSize * scale)
    val normY = scrollY + (screenY - screenHeight / 2f) / (mapSize * scale)
    return invertProjection(normX, normY)
}
