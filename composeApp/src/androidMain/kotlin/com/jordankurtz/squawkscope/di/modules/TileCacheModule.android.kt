package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.cache.FileTileCache
import com.jordankurtz.squawkscope.map.cache.JvmCacheFileSystem
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.map.cache.TileCacheDatabase
import com.jordankurtz.squawkscope.map.offline.AndroidThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.AndroidThumbnailGenerator
import com.jordankurtz.squawkscope.map.offline.ThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.io.File

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val cacheDir = File(contextWrapper.context.cacheDir, "map_tiles")
        val cacheFileSystem = JvmCacheFileSystem(cacheDir)
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
        AndroidThumbnailGenerator(
            tileCacheDir = File(contextWrapper.context.cacheDir, "map_tiles"),
            ioDispatcher = ioDispatcher,
        )

    @Single
    actual fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager =
        AndroidThumbnailFileManager(File(contextWrapper.context.cacheDir, "thumbnails"))
}
