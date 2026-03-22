package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapHelpersTest {
    @Test
    fun mapSizeAtLevelZero() {
        assertEquals(256, mapSizeAtLevel(0, tileSize = 256))
    }

    @Test
    fun mapSizeAtLevelOne() {
        assertEquals(512, mapSizeAtLevel(1, tileSize = 256))
    }

    @Test
    fun mapSizeScalesExponentially() {
        val size4 = mapSizeAtLevel(4, tileSize = 256)
        assertEquals(256 * 16, size4)
    }

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

    @Test
    fun doProjectionZeroZero() {
        val (x, y) = doProjection(0.0, 0.0)
        assertEquals(0.5, x, 0.001)
        assertEquals(0.5, y, 0.001)
    }

    @Test
    fun doProjectionReturnsNormalizedValues() {
        val (x, y) = doProjection(40.0, -100.0)
        assertTrue(x in 0.0..1.0, "x should be normalized: $x")
        assertTrue(y in 0.0..1.0, "y should be normalized: $y")
    }

    @Test
    fun doProjectionNegativeLongitudeIsWest() {
        val (xWest, _) = doProjection(0.0, -90.0)
        val (xEast, _) = doProjection(0.0, 90.0)
        assertTrue(xWest < xEast, "Western longitude should have smaller x")
    }

    @Test
    fun doProjectionNorthernLatitudeIsSmaller() {
        // In web Mercator normalized, north is y=0, south is y=1
        val (_, yNorth) = doProjection(60.0, 0.0)
        val (_, ySouth) = doProjection(-60.0, 0.0)
        assertTrue(yNorth < ySouth, "Northern latitude should have smaller y: $yNorth vs $ySouth")
    }

    @Test
    fun doProjectionInvalidLatitudeThrows() {
        assertFailsWith<IllegalStateException> {
            doProjection(91.0, 0.0)
        }
    }

    @Test
    fun doProjectionInvalidLongitudeThrows() {
        assertFailsWith<IllegalStateException> {
            doProjection(0.0, 181.0)
        }
    }

    @Test
    fun doProjectionNullLatitudeThrows() {
        assertFailsWith<IllegalStateException> {
            doProjection(null, 0.0)
        }
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
        // Verify projected coordinates match doProjection
        val (expectedX, expectedY) = doProjection(40.0, -74.0)
        assertEquals(expectedX, result.x, 0.0001)
        assertEquals(expectedY, result.y, 0.0001)
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

        // Compute expected projected values
        val projected = coordinates.map { (lat, lon) -> doProjection(lat, lon) }
        assertEquals(projected.minOf { it.first }, result.xLeft, 0.0001)
        assertEquals(projected.minOf { it.second }, result.yTop, 0.0001)
        assertEquals(projected.maxOf { it.first }, result.xRight, 0.0001)
        assertEquals(projected.maxOf { it.second }, result.yBottom, 0.0001)
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
        assertEquals(result.xLeft, result.xRight, 0.0001)
        assertEquals(result.yTop, result.yBottom, 0.0001)
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

        assertTrue(result.xLeft < result.xRight, "xLeft should be less than xRight")
        assertTrue(result.yTop < result.yBottom, "yTop should be less than yBottom (north is smaller y)")
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
