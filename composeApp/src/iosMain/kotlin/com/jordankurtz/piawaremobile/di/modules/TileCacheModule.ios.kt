package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.NoOpTileCache
import com.jordankurtz.piawaremobile.map.cache.TileCache
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache = NoOpTileCache()
}
