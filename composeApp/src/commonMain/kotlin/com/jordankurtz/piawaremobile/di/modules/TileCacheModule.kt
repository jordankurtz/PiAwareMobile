package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
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
}
