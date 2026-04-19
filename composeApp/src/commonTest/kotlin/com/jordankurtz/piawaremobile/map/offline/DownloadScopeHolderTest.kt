package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadScopeHolderTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `scope is active on creation`() {
        val holder = DownloadScopeHolder(testDispatcher)
        assertTrue(holder.scope.isActive)
    }

    @Test
    fun `close cancels the scope`() {
        val holder = DownloadScopeHolder(testDispatcher)
        holder.close()
        assertFalse(holder.scope.isActive)
    }
}
