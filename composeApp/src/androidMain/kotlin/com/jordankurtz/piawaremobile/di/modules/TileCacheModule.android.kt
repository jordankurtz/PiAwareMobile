package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.JvmCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.AndroidThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.AndroidThumbnailGenerator
import com.jordankurtz.piawaremobile.map.offline.ThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.ThumbnailGenerator
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
