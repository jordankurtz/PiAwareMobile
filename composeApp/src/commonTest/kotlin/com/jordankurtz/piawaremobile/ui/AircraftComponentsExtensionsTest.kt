package com.jordankurtz.piawaremobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AircraftComponentsExtensionsTest {
    @Test
    fun roundToZeroDecimals() {
        assertEquals(3.0, 3.14159.round(0))
    }

    @Test
    fun roundToTwoDecimals() {
        assertEquals(3.14, 3.14159.round(2))
    }

    @Test
    fun roundToFourDecimals() {
        assertEquals(40.1234, 40.12344.round(4))
    }

    @Test
    fun roundNegativeValues() {
        assertEquals(-100.5, (-100.4999).round(1))
    }

    @Test
    fun cardinalDirectionNorth() {
        assertEquals("N", 0.0.toCardinalDirection())
        assertEquals("N", 360.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionEast() {
        assertEquals("E", 90.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionSouth() {
        assertEquals("S", 180.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionWest() {
        assertEquals("W", 270.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionNortheast() {
        assertEquals("NE", 45.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionSouthwest() {
        assertEquals("SW", 225.0.toCardinalDirection())
    }

    @Test
    fun cardinalDirectionNearBoundary() {
        // 22 degrees is still N (boundary is 22.5)
        assertEquals("N", 22.0.toCardinalDirection())
        // 23 degrees should be NE
        assertEquals("NE", 23.0.toCardinalDirection())
    }
}
