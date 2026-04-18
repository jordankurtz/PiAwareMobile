package com.jordankurtz.piawaremobile.map.offline

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.cache.TileCacheQueries
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
    private lateinit var queries: TileCacheQueries
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        TileCacheDatabase.Schema.create(driver)
        queries = TileCacheDatabase(driver).tileCacheQueries
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
            insertTile(zoom = 10, col = 5, row = 3, providerId = "osm")
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
            insertTile(zoom = 5, col = 10, row = 20, providerId = "osm")
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
            insertTile(zoom = 5, col = 10, row = 20, providerId = "osm")
            insertTile(zoom = 5, col = 11, row = 20, providerId = "osm")
            store.pinTile(zoomLevel = 5, col = 10, row = 20, regionId = id, providerId = "osm")
            store.pinTile(zoomLevel = 5, col = 11, row = 20, regionId = id, providerId = "osm")

            val tiles = store.getPinnedTilesForRegion(id)
            assertEquals(2, tiles.size)
            val coords = tiles.toSet()
            assertTrue(TileCoord(zoom = 5, col = 10, row = 20) in coords)
            assertTrue(TileCoord(zoom = 5, col = 11, row = 20) in coords)
        }

    @Test
    fun `updateRegionStats persists tile count and size`() =
        runTest(testDispatcher) {
            val id =
                store.saveRegion(
                    OfflineRegion(
                        name = "Stats",
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
            store.updateRegionStats(id = id, tileCount = 42L, sizeBytes = 630_000L)
            val region = store.getRegion(id)
            assertEquals(42L, region?.tileCount)
            assertEquals(630_000L, region?.sizeBytes)
        }

    @Test
    fun `updateDownloadStatus persists status and downloaded tile count`() =
        runTest(testDispatcher) {
            val id =
                store.saveRegion(
                    OfflineRegion(
                        name = "Status",
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
            store.updateDownloadStatus(id, DownloadStatus.DOWNLOADING, 5L)
            val region = store.getRegion(id)
            assertEquals(DownloadStatus.DOWNLOADING, region?.status)
            assertEquals(5L, region?.downloadedTileCount)
        }

    @Test
    fun `getExclusiveTilesForRegion excludes shared tiles`() =
        runTest(testDispatcher) {
            val region1Id = store.saveRegion(baseRegion("R1"))
            val region2Id = store.saveRegion(baseRegion("R2"))

            // Tile (8, 10, 20) pinned by both regions
            insertTile(zoom = 8, col = 10, row = 20, providerId = "osm")
            store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = region1Id, providerId = "osm")
            store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = region2Id, providerId = "osm")
            // Tile (8, 11, 20) exclusively pinned by region1
            insertTile(zoom = 8, col = 11, row = 20, providerId = "osm")
            store.pinTile(zoomLevel = 8, col = 11, row = 20, regionId = region1Id, providerId = "osm")

            val exclusive = store.getExclusiveTilesForRegion(region1Id)
            assertEquals(1, exclusive.size)
            assertEquals(TileCoord(zoom = 8, col = 11, row = 20), exclusive[0])
        }

    @Test
    fun `getFreedBytesForRegion returns 0 when no tiles pinned`() =
        runTest(testDispatcher) {
            val id = store.saveRegion(baseRegion("R"))
            // No tiles pinned — nothing to free
            val freed = store.getFreedBytesForRegion(id)
            assertEquals(0L, freed)
        }

    @Test
    fun `getFreedBytesForRegion returns size of exclusively pinned tiles`() =
        runTest(testDispatcher) {
            val id = store.saveRegion(baseRegion("R"))
            insertTile(zoom = 8, col = 10, row = 20, providerId = "osm", sizeBytes = 512L)
            store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = id, providerId = "osm")
            val freed = store.getFreedBytesForRegion(id)
            assertEquals(512L, freed)
        }

    @Test
    fun `resetStuckDownloads transitions DOWNLOADING to PARTIAL`() =
        runTest(testDispatcher) {
            val id = store.saveRegion(baseRegion("R"))
            store.updateDownloadStatus(id, DownloadStatus.DOWNLOADING, 5L)

            store.resetStuckDownloads()

            val region = store.getRegion(id)
            assertEquals(DownloadStatus.PARTIAL, region?.status)
            assertEquals(5L, region?.downloadedTileCount)
        }

    @Test
    fun `resetStuckDownloads leaves COMPLETE, FAILED, and PARTIAL regions unchanged`() =
        runTest(testDispatcher) {
            val completeId = store.saveRegion(baseRegion("Complete"))
            val failedId = store.saveRegion(baseRegion("Failed"))
            val partialId = store.saveRegion(baseRegion("Partial"))

            store.updateDownloadStatus(failedId, DownloadStatus.FAILED, 0L)
            store.updateDownloadStatus(partialId, DownloadStatus.PARTIAL, 3L)

            store.resetStuckDownloads()

            assertEquals(DownloadStatus.COMPLETE, store.getRegion(completeId)?.status)
            assertEquals(DownloadStatus.FAILED, store.getRegion(failedId)?.status)
            assertEquals(DownloadStatus.PARTIAL, store.getRegion(partialId)?.status)
        }

    @Test
    fun `deleteRegion cascades to pinned tiles via FK without explicit cleanup`() =
        runTest(testDispatcher) {
            val id = store.saveRegion(baseRegion("Cascade"))
            insertTile(zoom = 8, col = 3, row = 4, providerId = "osm")
            store.pinTile(zoomLevel = 8, col = 3, row = 4, regionId = id, providerId = "osm")
            assertTrue(store.isPinned(zoomLevel = 8, col = 3, row = 4, providerId = "osm"))

            // Delete directly via SQL, bypassing the explicit deletePinnedTilesByRegion workaround
            queries.deleteRegion(id)

            // FK cascade should remove pinned_tile rows automatically
            assertFalse(store.isPinned(zoomLevel = 8, col = 3, row = 4, providerId = "osm"))
        }

    private fun insertTile(
        zoom: Int,
        col: Int,
        row: Int,
        providerId: String,
        sizeBytes: Long = 1024L,
    ) {
        queries.upsertTile(
            zoom_level = zoom.toLong(),
            col = col.toLong(),
            row = row.toLong(),
            provider_id = providerId,
            size_bytes = sizeBytes,
            fetched_at = 0L,
        )
    }

    private fun baseRegion(name: String) =
        OfflineRegion(
            name = name,
            minZoom = 8,
            maxZoom = 12,
            minLat = 0.0,
            maxLat = 1.0,
            minLon = 0.0,
            maxLon = 1.0,
            providerId = "osm",
            createdAt = 1000L,
        )
}
