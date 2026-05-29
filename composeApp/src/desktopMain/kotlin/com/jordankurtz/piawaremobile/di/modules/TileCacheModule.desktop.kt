package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.JvmCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.DesktopThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.DesktopThumbnailGenerator
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
        val cacheDir = desktopCacheDir()
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
        DesktopThumbnailGenerator(
            tileCacheDir = desktopCacheDir(),
            ioDispatcher = ioDispatcher,
        )

    @Single
    actual fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager =
        DesktopThumbnailFileManager(File(desktopCacheDir().parent, "thumbnails"))
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

internal fun desktopDbDir(): File {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    return when {
        osName.contains("mac") ->
            File(userHome, "Library/Caches/PiAwareMobile/db")
        osName.contains("win") -> {
            val localAppData =
                System.getenv("LOCALAPPDATA")
                    ?: File(userHome, "AppData/Local").path
            File(localAppData, "PiAwareMobile/db")
        }
        else -> {
            val xdgCacheHome =
                System.getenv("XDG_CACHE_HOME")
                    ?: File(userHome, ".cache").path
            File(xdgCacheHome, "PiAwareMobile/db")
        }
    }
}
