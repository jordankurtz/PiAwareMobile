package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.cache.AppleCacheFileSystem
import com.jordankurtz.squawkscope.map.cache.FileTileCache
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.map.cache.TileCacheDatabase
import com.jordankurtz.squawkscope.map.offline.AppleThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.IosThumbnailGenerator
import com.jordankurtz.squawkscope.map.offline.ThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.ThumbnailGenerator
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
        val cacheDir = appleCacheDir()
        val cacheFileSystem = AppleCacheFileSystem(cacheDir)
        return FileTileCache(
            cacheFileSystem = cacheFileSystem,
            queries = database.tileCacheQueries,
            ioDispatcher = ioDispatcher,
        )
    }

    @Single
    actual fun provideThumbnailGenerator(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): ThumbnailGenerator =
        IosThumbnailGenerator(
            tileCacheDir = appleCacheDir(),
            ioDispatcher = ioDispatcher,
        )

    @Single
    actual fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager =
        AppleThumbnailFileManager(appleThumbnailDir())
}

private fun appleCacheDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String

    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("map_tiles")
}

private fun appleThumbnailDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String

    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("thumbnails")
}
