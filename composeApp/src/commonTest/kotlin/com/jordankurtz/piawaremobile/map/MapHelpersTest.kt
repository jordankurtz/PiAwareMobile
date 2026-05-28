package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapHelpersTest {
    @Test
    fun getColorForAltitudeGround() {
        assertEquals(Color(139, 69, 19), getColorForAltitude(GROUND_ALTITUDE))
    }

    @Test
    fun getColorForAltitudeNull() {
        // null altitude should use 0 → red range
        assertEquals(Color(255, 64, 0), getColorForAltitude(null))
    }

    @Test
    fun getColorForAltitudeLowAltitude() {
        assertEquals(Color(255, 64, 0), getColorForAltitude("100"))
    }

    @Test
    fun getColorForAltitudeCruisingAltitude() {
        // 35000 ft is in 30001..35000 range
        assertEquals(Color(0, 64, 255), getColorForAltitude("35000"))
    }

    @Test
    fun getColorForAltitudeVeryHigh() {
        // Above 50000 → purple
        assertEquals(Color(192, 0, 255), getColorForAltitude("60000"))
    }

    @Test
    fun getColorForAltitudeNonNumeric() {
        // Non-numeric string → toIntOrNull returns null → defaults to 0
        assertEquals(Color(255, 64, 0), getColorForAltitude("abc"))
    }

    // --- computeFitTarget tests ---

    @Test
    fun computeFitTargetEmptyListReturnsNull() {
        assertNull(computeFitTarget(emptyList()))
    }

    @Test
    fun computeFitTargetSingleAircraftReturnsSinglePoint() {
        val result = computeFitTarget(listOf(40.0 to -74.0))
        assertIs<FitTarget.SinglePoint>(result)
        // Verify raw lat/lon is returned without projection
        assertEquals(40.0, result.latitude, 0.0001)
        assertEquals(-74.0, result.longitude, 0.0001)
    }

    @Test
    fun computeFitTargetMultipleAircraftReturnsBoundingRegion() {
        val coordinates =
            listOf(
                40.0 to -74.0,
                34.0 to -118.0,
                41.0 to -87.0,
            )
        val result = computeFitTarget(coordinates)
        assertIs<FitTarget.BoundingRegion>(result)

        // Expected lat/lon bounds
        assertEquals(coordinates.maxOf { it.first }, result.north, 0.0001)
        assertEquals(coordinates.minOf { it.first }, result.south, 0.0001)
        assertEquals(coordinates.maxOf { it.second }, result.east, 0.0001)
        assertEquals(coordinates.minOf { it.second }, result.west, 0.0001)
    }

    @Test
    fun computeFitTargetTwoAircraftSameLocationReturnsDegenerateBoundingRegion() {
        val coordinates =
            listOf(
                40.0 to -74.0,
                40.0 to -74.0,
            )
        val result = computeFitTarget(coordinates)
        assertIs<FitTarget.BoundingRegion>(result)

        // Degenerate bounding box: all edges are the same point
        assertEquals(result.north, result.south, 0.0001)
        assertEquals(result.east, result.west, 0.0001)
    }

    @Test
    fun computeFitTargetBoundingRegionHasCorrectOrientation() {
        // North-west to south-east: xLeft < xRight, yTop < yBottom
        val coordinates =
            listOf(
                50.0 to -10.0,
                30.0 to 20.0,
            )
        val result = computeFitTarget(coordinates)
        assertIs<FitTarget.BoundingRegion>(result)

        assertTrue(result.south < result.north, "south should be less than north")
        assertTrue(result.west < result.east, "west should be less than east")
    }

    @Test
    fun computeFitTargetTwoAircraftReturnsBoundingRegionNotSinglePoint() {
        // Even with just 2 aircraft, we should get a BoundingRegion, not SinglePoint
        val coordinates =
            listOf(
                40.0 to -74.0,
                34.0 to -118.0,
            )
        val result = computeFitTarget(coordinates)
        assertIs<FitTarget.BoundingRegion>(result)
    }
}
