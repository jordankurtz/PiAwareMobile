package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.cache.TileCacheDatabase
import com.jordankurtz.squawkscope.map.offline.OfflineTileStore
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
expect class DatabaseModule() {
    @Single
    fun provideTileCacheDatabase(contextWrapper: ContextWrapper): TileCacheDatabase

    @Single
    fun provideOfflineTileStore(
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): OfflineTileStore
}
