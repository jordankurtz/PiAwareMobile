package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.IosCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.TileCache
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
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val cachePaths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true,
        )
        val baseCacheDir = cachePaths.first() as String
        @Suppress("CAST_NEVER_SUCCEEDS")
        val cacheDir = (baseCacheDir as NSString).stringByAppendingPathComponent("map_tiles")
        val cacheFileSystem = IosCacheFileSystem(cacheDir)
        return FileTileCache(cacheFileSystem = cacheFileSystem, ioDispatcher = ioDispatcher)
    }
}
