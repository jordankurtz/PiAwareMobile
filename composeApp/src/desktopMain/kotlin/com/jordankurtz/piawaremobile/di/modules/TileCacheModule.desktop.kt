package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.TileCache
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.io.File

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val userHome = System.getProperty("user.home")
        val cacheDir = File(userHome, ".cache/piawaremobile/map_tiles")
        return FileTileCache(cacheDir = cacheDir, ioDispatcher = ioDispatcher)
    }
}
