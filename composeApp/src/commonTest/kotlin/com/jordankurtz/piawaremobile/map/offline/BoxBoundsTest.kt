package com.jordankurtz.piawaremobile.map.offline

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxBoundsTest {
    // A 200×100 box positioned at (100, 50) in a 500×400 screen
    private val bounds = BoxBounds(left = 100f, top = 50f, right = 300f, bottom = 150f)
    private val screenWidth = 500f
    private val screenHeight = 400f

    // --- BoxBounds.translate ---

    @Test
    fun translateNormalDeltaWithinBounds() {
        val result = bounds.translate(dx = 10f, dy = 20f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(110f, result.left, 0.001f)
        assertEquals(70f, result.top, 0.001f)
        assertEquals(310f, result.right, 0.001f)
        assertEquals(170f, result.bottom, 0.001f)
    }

    @Test
    fun translateClampsAtLeftEdge() {
        // dx = -200 would push left to -100, should clamp left to 0
        val result = bounds.translate(dx = -200f, dy = 0f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(0f, result.left, 0.001f)
    }

    @Test
    fun translateClampsAtRightEdge() {
        // dx = 300 would push right to 600, past screenWidth=500; right should be clamped to 500
        val result = bounds.translate(dx = 300f, dy = 0f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(screenWidth, result.right, 0.001f)
    }

    @Test
    fun translateClampsAtTopEdge() {
        // dy = -200 would push top to -150, should clamp top to 0
        val result = bounds.translate(dx = 0f, dy = -200f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(0f, result.top, 0.001f)
    }

    @Test
    fun translateClampsAtBottomEdge() {
        // dy = 400 would push bottom to 550, past screenHeight=400; bottom should be clamped to 400
        val result = bounds.translate(dx = 0f, dy = 400f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(screenHeight, result.bottom, 0.001f)
    }

    @Test
    fun translatePreservesBoxWidthAfterLeftClamp() {
        val boxWidth = bounds.right - bounds.left
        val result = bounds.translate(dx = -200f, dy = 0f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(boxWidth, result.right - result.left, 0.001f)
    }

    @Test
    fun translatePreservesBoxWidthAfterRightClamp() {
        val boxWidth = bounds.right - bounds.left
        val result = bounds.translate(dx = 300f, dy = 0f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(boxWidth, result.right - result.left, 0.001f)
    }

    @Test
    fun translatePreservesBoxHeightAfterTopClamp() {
        val boxHeight = bounds.bottom - bounds.top
        val result = bounds.translate(dx = 0f, dy = -200f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(boxHeight, result.bottom - result.top, 0.001f)
    }

    @Test
    fun translatePreservesBoxHeightAfterBottomClamp() {
        val boxHeight = bounds.bottom - bounds.top
        val result = bounds.translate(dx = 0f, dy = 400f, screenWidth = screenWidth, screenHeight = screenHeight)
        assertEquals(boxHeight, result.bottom - result.top, 0.001f)
    }

    // --- isInsideBox ---

    @Test
    fun isInsideBoxReturnsTrueForPointInsideBox() {
        assertTrue(isInsideBox(Offset(200f, 100f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointLeftOfBox() {
        assertFalse(isInsideBox(Offset(50f, 100f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointRightOfBox() {
        assertFalse(isInsideBox(Offset(350f, 100f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointAboveBox() {
        assertFalse(isInsideBox(Offset(200f, 20f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointBelowBox() {
        assertFalse(isInsideBox(Offset(200f, 200f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointOnLeftEdge() {
        // Boundary uses strict inequality (>), so the edge itself is not inside
        assertFalse(isInsideBox(Offset(100f, 100f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointOnRightEdge() {
        assertFalse(isInsideBox(Offset(300f, 100f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointOnTopEdge() {
        assertFalse(isInsideBox(Offset(200f, 50f), bounds))
    }

    @Test
    fun isInsideBoxReturnsFalseForPointOnBottomEdge() {
        assertFalse(isInsideBox(Offset(200f, 150f), bounds))
    }
}
