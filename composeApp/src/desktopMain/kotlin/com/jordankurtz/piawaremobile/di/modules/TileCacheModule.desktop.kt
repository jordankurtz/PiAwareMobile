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
        val cacheDir = desktopCacheDir()
        return FileTileCache(cacheDir = cacheDir, ioDispatcher = ioDispatcher)
    }
}

internal fun desktopCacheDir(): File {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    return when {
        osName.contains("mac") ->
            File(userHome, "Library/Caches/PiAwareMobile/tiles")
        osName.contains("win") -> {
            val localAppData =
                System.getenv("LOCALAPPDATA")
                    ?: File(userHome, "AppData/Local").path
            File(localAppData, "PiAwareMobile/tiles")
        }
        else -> {
            val xdgCacheHome =
                System.getenv("XDG_CACHE_HOME")
                    ?: File(userHome, ".cache").path
            File(xdgCacheHome, "PiAwareMobile/tiles")
        }
    }
}
