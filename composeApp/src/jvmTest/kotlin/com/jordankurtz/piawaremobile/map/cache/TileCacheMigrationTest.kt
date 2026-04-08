package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that all schema migrations can be applied cleanly in sequence,
 * starting from v0 (before any migrations were added) through the current version.
 *
 * v0 schema: tile and cache_entry without provider_id; no offline_region or pinned_tile.
 * Migration 1: adds offline_region and pinned_tile tables.
 * Migration 2: adds status and downloaded_tile_count columns to offline_region.
 * Migration 3: recreates tile, cache_entry, pinned_tile with provider_id in PKs.
 */
class TileCacheMigrationTest {
    @Test
    fun `all migrations apply cleanly from v0 to current version`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Create v0 schema: tile and cache_entry without provider_id
        driver.execute(null, V0_TILE, 0)
        driver.execute(null, V0_CACHE_ENTRY, 0)
        driver.execute(null, V0_CACHE_ENTRY_INDEX, 0)

        // Insert a row in v0 so we can verify data is preserved through the migration
        driver.execute(null, "INSERT INTO tile VALUES (10, 5, 3, 256, 1000)", 0)
        driver.execute(null, "INSERT INTO cache_entry VALUES (10, 5, 3, 1000)", 0)

        TileCacheDatabase.Schema.migrate(driver, 0, TileCacheDatabase.Schema.version)

        val db = TileCacheDatabase(driver)
        val queries = db.tileCacheQueries

        // Verify that the pre-existing tile is accessible via the migrated schema
        val cacheEntry =
            queries.selectCacheEntry(
                zoom_level = 10L,
                col = 5L,
                row = 3L,
                provider_id = "openstreetmap",
            ).executeAsOneOrNull()
        assertEquals(1000L, cacheEntry?.fetched_at, "Pre-existing tile should be readable after migration")

        // Verify offline_region table with all expected columns
        queries.insertOfflineRegion(
            name = "Test Region",
            min_zoom = 8L,
            max_zoom = 12L,
            min_lat = 37.0,
            max_lat = 38.0,
            min_lon = -122.0,
            max_lon = -121.0,
            provider_id = "openstreetmap",
            created_at = 2000L,
        )
        val regionId = queries.lastInsertedRegionId().executeAsOne()
        val region = queries.selectRegionById(regionId).executeAsOneOrNull()
        assertEquals("Test Region", region?.name)
        assertEquals("COMPLETE", region?.status)
        assertEquals(0L, region?.downloaded_tile_count)

        // Verify pinned_tile accepts provider_id in its composite PK
        queries.upsertTile(
            zoom_level = 10L,
            col = 5L,
            row = 3L,
            provider_id = "openstreetmap",
            size_bytes = 512L,
            fetched_at = 3000L,
        )
        queries.insertPinnedTile(
            zoom_level = 10L,
            col = 5L,
            row = 3L,
            provider_id = "openstreetmap",
            region_id = regionId,
        )
        assertEquals(
            1L,
            queries.isPinned(
                zoom_level = 10L,
                col = 5L,
                row = 3L,
                provider_id = "openstreetmap",
            ).executeAsOne(),
        )
    }

    companion object {
        private val V0_TILE =
            """
            CREATE TABLE tile (
                zoom_level INTEGER NOT NULL,
                col        INTEGER NOT NULL,
                row        INTEGER NOT NULL,
                size_bytes INTEGER NOT NULL,
                fetched_at INTEGER NOT NULL,
                PRIMARY KEY (zoom_level, col, row)
            )
            """.trimIndent()

        private val V0_CACHE_ENTRY =
            """
            CREATE TABLE cache_entry (
                zoom_level    INTEGER NOT NULL,
                col           INTEGER NOT NULL,
                row           INTEGER NOT NULL,
                last_accessed INTEGER NOT NULL,
                PRIMARY KEY (zoom_level, col, row),
                FOREIGN KEY (zoom_level, col, row)
                    REFERENCES tile(zoom_level, col, row) ON DELETE CASCADE
            )
            """.trimIndent()

        private const val V0_CACHE_ENTRY_INDEX =
            "CREATE INDEX cache_entry_lru ON cache_entry(last_accessed ASC)"
    }
}
