package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.map.cache.TileCacheDatabase
import com.jordankurtz.squawkscope.map.offline.ThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
expect class TileCacheModule() {
    @Single
    fun provideTileCache(
        contextWrapper: ContextWrapper,
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache

    @Single
    fun provideThumbnailGenerator(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): ThumbnailGenerator

    @Single
    fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager
}
