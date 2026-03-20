package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowSizeTest {
    @Test
    fun compactWidthForNarrowScreens() {
        val windowSize = WindowSize(400.dp, 800.dp)
        assertEquals(WindowSizeClass.Compact, windowSize.widthSizeClass)
    }

    @Test
    fun mediumWidthForMidSizeScreens() {
        val windowSize = WindowSize(700.dp, 800.dp)
        assertEquals(WindowSizeClass.Medium, windowSize.widthSizeClass)
    }

    @Test
    fun expandedWidthForWideScreens() {
        val windowSize = WindowSize(900.dp, 800.dp)
        assertEquals(WindowSizeClass.Expanded, windowSize.widthSizeClass)
    }

    @Test
    fun compactBoundaryIsExclusive() {
        val justBelow = WindowSize(599.dp, 800.dp)
        assertEquals(WindowSizeClass.Compact, justBelow.widthSizeClass)

        val atBoundary = WindowSize(600.dp, 800.dp)
        assertEquals(WindowSizeClass.Medium, atBoundary.widthSizeClass)
    }

    @Test
    fun mediumBoundaryIsExclusive() {
        val justBelow = WindowSize(839.dp, 800.dp)
        assertEquals(WindowSizeClass.Medium, justBelow.widthSizeClass)

        val atBoundary = WindowSize(840.dp, 800.dp)
        assertEquals(WindowSizeClass.Expanded, atBoundary.widthSizeClass)
    }

    @Test
    fun isTabletFalseForCompact() {
        val windowSize = WindowSize(400.dp, 800.dp)
        assertFalse(windowSize.isTablet)
    }

    @Test
    fun isTabletTrueForMedium() {
        val windowSize = WindowSize(700.dp, 800.dp)
        assertTrue(windowSize.isTablet)
    }

    @Test
    fun isTabletTrueForExpanded() {
        val windowSize = WindowSize(900.dp, 800.dp)
        assertTrue(windowSize.isTablet)
    }

    @Test
    fun zeroWidthIsCompact() {
        val windowSize = WindowSize(0.dp, 0.dp)
        assertEquals(WindowSizeClass.Compact, windowSize.widthSizeClass)
        assertFalse(windowSize.isTablet)
    }
}
