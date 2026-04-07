package com.jordankurtz.piawaremobile.map.offline

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightOfflineTileStoreTest {
    private lateinit var store: SqlDelightOfflineTileStore
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TileCacheDatabase.Schema.create(driver)
        val queries = TileCacheDatabase(driver).tileCacheQueries
        store = SqlDelightOfflineTileStore(queries, testDispatcher)
    }

    @Test
    fun `saveRegion returns a positive id`() =
        runTest(testDispatcher) {
            val region =
                OfflineRegion(
                    name = "Test",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 37.0,
                    maxLat = 38.0,
                    minLon = -122.0,
                    maxLon = -121.0,
                    providerId = "osm",
                    createdAt = 1000L,
                )
            val id = store.saveRegion(region)
            assertEquals(1L, id)
        }

    @Test
    fun `getRegions returns saved regions`() =
        runTest(testDispatcher) {
            val region =
                OfflineRegion(
                    name = "Home",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 37.0,
                    maxLat = 38.0,
                    minLon = -122.0,
                    maxLon = -121.0,
                    providerId = "osm",
                    createdAt = 1000L,
                )
            store.saveRegion(region)
            val regions = store.getRegions()
            assertEquals(1, regions.size)
            assertEquals("Home", regions[0].name)
            assertEquals(8, regions[0].minZoom)
            assertEquals("osm", regions[0].providerId)
        }

    @Test
    fun `getRegion returns null for unknown id`() =
        runTest(testDispatcher) {
            assertNull(store.getRegion(999L))
        }

    @Test
    fun `deleteRegion removes region and cascades to pinned tiles`() =
        runTest(testDispatcher) {
            val id =
                store.saveRegion(
                    OfflineRegion(
                        name = "Temp",
                        minZoom = 8,
                        maxZoom = 12,
                        minLat = 0.0,
                        maxLat = 1.0,
                        minLon = 0.0,
                        maxLon = 1.0,
                        providerId = "osm",
                        createdAt = 1000L,
                    ),
                )
            store.pinTile(zoomLevel = 10, col = 5, row = 3, regionId = id, providerId = "osm")
            assertTrue(store.isPinned(zoomLevel = 10, col = 5, row = 3, providerId = "osm"))

            store.deleteRegion(id)

            assertNull(store.getRegion(id))
            assertFalse(store.isPinned(zoomLevel = 10, col = 5, row = 3, providerId = "osm"))
        }

    @Test
    fun `isPinned returns true after pinTile`() =
        runTest(testDispatcher) {
            val id =
                store.saveRegion(
                    OfflineRegion(
                        name = "R",
                        minZoom = 8,
                        maxZoom = 12,
                        minLat = 0.0,
                        maxLat = 1.0,
                        minLon = 0.0,
                        maxLon = 1.0,
                        providerId = "osm",
                        createdAt = 1000L,
                    ),
                )
            assertFalse(store.isPinned(zoomLevel = 5, col = 10, row = 20, providerId = "osm"))
            store.pinTile(zoomLevel = 5, col = 10, row = 20, regionId = id, providerId = "osm")
            assertTrue(store.isPinned(zoomLevel = 5, col = 10, row = 20, providerId = "osm"))
        }

    @Test
    fun `getPinnedTilesForRegion returns all pinned tiles`() =
        runTest(testDispatcher) {
            val id =
                store.saveRegion(
                    OfflineRegion(
                        name = "R",
                        minZoom = 8,
                        maxZoom = 12,
                        minLat = 0.0,
                        maxLat = 1.0,
                        minLon = 0.0,
                        maxLon = 1.0,
                        providerId = "osm",
                        createdAt = 1000L,
                    ),
                )
            store.pinTile(zoomLevel = 5, col = 10, row = 20, regionId = id, providerId = "osm")
            store.pinTile(zoomLevel = 5, col = 11, row = 20, regionId = id, providerId = "osm")

            val tiles = store.getPinnedTilesForRegion(id)
            assertEquals(2, tiles.size)
            val coords = tiles.map { Triple(it.first, it.second, it.third) }.toSet()
            assertTrue(Triple(5, 10, 20) in coords)
            assertTrue(Triple(5, 11, 20) in coords)
        }
}
