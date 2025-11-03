package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.graphics.Color
import com.jordankurtz.piawaremobile.model.Location
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

const val MAX_LEVEL = 16
const val MIN_LEVEL = 1

const val TILE_SIZE = 256
private const val X0 = -2.0037508342789248E7
val mapSize = mapSizeAtLevel(MAX_LEVEL, tileSize = TILE_SIZE)

const val GROUND_ALTITUDE = "ground"

fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
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

val Location.projected: Pair<Double, Double>
    get() = doProjection(latitude, longitude)

fun doProjection(latitude: Double?, longitude: Double?): Pair<Double, Double> {
    if (latitude == null || longitude == null || abs(latitude) > 90 || abs(longitude) > 180) {
        error("Invalid latitude or longitude")
    }

    val num = longitude * 0.017453292519943295 // 2*pi / 360
    val a = latitude * 0.017453292519943295

    val x = normalize(6378137.0 * num, min = X0, max = -X0)
    val y = normalize(3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a))), min = -X0, max = X0)

    return Pair(x, y)
}

private fun normalize(t: Double, min: Double, max: Double): Double {
    return (t - min) / (max - min)
}
