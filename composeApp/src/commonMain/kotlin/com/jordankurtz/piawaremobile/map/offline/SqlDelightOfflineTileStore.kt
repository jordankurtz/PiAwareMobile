package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.map.cache.Offline_region
import com.jordankurtz.piawaremobile.map.cache.TileCacheQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SqlDelightOfflineTileStore(
    private val queries: TileCacheQueries,
    private val ioDispatcher: CoroutineDispatcher,
) : OfflineTileStore {
    override suspend fun saveRegion(region: OfflineRegion): Long =
        withContext(ioDispatcher) {
            queries.transactionWithResult {
                queries.insertOfflineRegion(
                    name = region.name,
                    min_zoom = region.minZoom.toLong(),
                    max_zoom = region.maxZoom.toLong(),
                    min_lat = region.minLat,
                    max_lat = region.maxLat,
                    min_lon = region.minLon,
                    max_lon = region.maxLon,
                    provider_id = region.providerId,
                    created_at = region.createdAt,
                )
                queries.lastInsertedRegionId().executeAsOne()
            }
        }

    override suspend fun getRegions(): List<OfflineRegion> =
        withContext(ioDispatcher) {
            queries.selectAllRegions().executeAsList().map { it.toOfflineRegion() }
        }

    override suspend fun getRegion(id: Long): OfflineRegion? =
        withContext(ioDispatcher) {
            queries.selectRegionById(id).executeAsOneOrNull()?.toOfflineRegion()
        }

    override suspend fun deleteRegion(id: Long): Unit =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.deletePinnedTilesByRegion(id)
                queries.deleteRegion(id)
            }
        }

    override suspend fun pinTile(
        zoomLevel: Int,
        col: Int,
        row: Int,
        regionId: Long,
    ): Unit =
        withContext(ioDispatcher) {
            queries.insertPinnedTile(
                zoom_level = zoomLevel.toLong(),
                col = col.toLong(),
                row = row.toLong(),
                region_id = regionId,
            )
        }

    override suspend fun isPinned(
        zoomLevel: Int,
        col: Int,
        row: Int,
    ): Boolean =
        withContext(ioDispatcher) {
            queries.isPinned(
                zoom_level = zoomLevel.toLong(),
                col = col.toLong(),
                row = row.toLong(),
            ).executeAsOne() > 0L
        }

    override suspend fun getPinnedTilesForRegion(regionId: Long): List<Triple<Int, Int, Int>> =
        withContext(ioDispatcher) {
            queries.selectPinnedTilesByRegion(regionId).executeAsList().map {
                Triple(it.zoom_level.toInt(), it.col.toInt(), it.row.toInt())
            }
        }
}

private fun Offline_region.toOfflineRegion() =
    OfflineRegion(
        id = id,
        name = name,
        minZoom = min_zoom.toInt(),
        maxZoom = max_zoom.toInt(),
        minLat = min_lat,
        maxLat = max_lat,
        minLon = min_lon,
        maxLon = max_lon,
        providerId = provider_id,
        createdAt = created_at,
        tileCount = tile_count,
        sizeBytes = size_bytes,
    )
