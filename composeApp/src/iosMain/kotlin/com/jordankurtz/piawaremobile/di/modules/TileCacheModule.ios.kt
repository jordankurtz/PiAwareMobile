package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.IosCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.IosThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.IosThumbnailGenerator
import com.jordankurtz.piawaremobile.map.offline.ThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val cacheDir = iosCacheDir()
        val cacheFileSystem = IosCacheFileSystem(cacheDir)
        return FileTileCache(
            cacheFileSystem = cacheFileSystem,
            queries = database.tileCacheQueries,
            ioDispatcher = ioDispatcher,
        )
    }

    @Single
    fun provideThumbnailGenerator(
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): ThumbnailGenerator = IosThumbnailGenerator(
        tileCacheDir = iosCacheDir(),
        ioDispatcher = ioDispatcher,
    )

    @Single
    fun provideThumbnailFileManager(): ThumbnailFileManager =
        IosThumbnailFileManager(iosThumbnailDir())
}

private fun iosCacheDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String

    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("map_tiles")
}

private fun iosThumbnailDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String

    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("thumbnails")
}
