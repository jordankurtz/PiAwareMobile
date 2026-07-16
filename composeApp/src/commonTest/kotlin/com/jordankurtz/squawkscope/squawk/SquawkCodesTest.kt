package com.jordankurtz.squawkscope.squawk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SquawkCodesTest {
    @Test
    fun `7700 is EMERGENCY`() {
        val info = SquawkCodes["7700"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `7600 is EMERGENCY`() {
        val info = SquawkCodes["7600"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `7500 is EMERGENCY`() {
        val info = SquawkCodes["7500"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.EMERGENCY, info.severity)
    }

    @Test
    fun `1200 is INFO`() {
        val info = SquawkCodes["1200"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.INFO, info.severity)
    }

    @Test
    fun `7400 is CAUTION`() {
        val info = SquawkCodes["7400"]
        assertNotNull(info)
        assertEquals(SquawkSeverity.CAUTION, info.severity)
    }

    @Test
    fun `unknown code returns null`() {
        assertNull(SquawkCodes["9999"])
    }

    @Test
    fun `at least one entry per severity bucket`() {
        val severities =
            SquawkCodes.all.values
                .map { it.severity }
                .toSet()
        assertTrue(SquawkSeverity.EMERGENCY in severities)
        assertTrue(SquawkSeverity.CAUTION in severities)
        assertTrue(SquawkSeverity.INFO in severities)
    }

    @Test
    fun `all entries have non-blank name and description`() {
        SquawkCodes.all.forEach { (code, info) ->
            assertTrue(info.name.isNotBlank(), "Code $code has blank name")
            assertTrue(info.description.isNotBlank(), "Code $code has blank description")
        }
    }
}
