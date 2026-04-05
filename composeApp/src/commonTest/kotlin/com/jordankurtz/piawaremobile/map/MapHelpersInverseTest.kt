package com.jordankurtz.piawaremobile.map

import kotlin.math.abs
import kotlin.test.Test

class MapHelpersInverseTest {
    private fun assertNearlyEqual(
        expected: Double,
        actual: Double,
        delta: Double = 1e-6,
        message: String = "",
    ) {
        val diff = abs(expected - actual)
        if (diff > delta) {
            throw AssertionError(
                "$message expected=$expected actual=$actual diff=$diff (tolerance=$delta)",
            )
        }
    }

    @Test
    fun `invertProjection round-trips with doProjection for equator`() {
        val (normX, normY) = doProjection(0.0, 0.0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(0.0, lat, message = "lat")
        assertNearlyEqual(0.0, lon, message = "lon")
    }

    @Test
    fun `invertProjection round-trips for San Francisco`() {
        val lat0 = 37.7749
        val lon0 = -122.4194
        val (normX, normY) = doProjection(lat0, lon0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(lat0, lat, message = "lat")
        assertNearlyEqual(lon0, lon, message = "lon")
    }

    @Test
    fun `invertProjection round-trips for negative latitude`() {
        val lat0 = -33.8688
        val lon0 = 151.2093
        val (normX, normY) = doProjection(lat0, lon0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(lat0, lat, message = "lat")
        assertNearlyEqual(lon0, lon, message = "lon")
    }

    @Test
    fun `screenToLatLon returns scroll center when screen center passed`() {
        val (lat, lon) =
            screenToLatLon(
                screenX = 500f,
                screenY = 400f,
                screenWidth = 1000f,
                screenHeight = 800f,
                scrollX = 0.5,
                scrollY = 0.5,
                scale = 1f,
            )
        assertNearlyEqual(0.0, lat, delta = 1e-4, message = "lat at center")
        assertNearlyEqual(0.0, lon, delta = 1e-4, message = "lon at center")
    }
}
