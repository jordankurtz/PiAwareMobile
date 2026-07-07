package com.jordankurtz.squawkscope.di.modules

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.cache.FileTileCache
import com.jordankurtz.squawkscope.map.cache.JvmCacheFileSystem
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.map.cache.TileCacheDatabase
import com.jordankurtz.squawkscope.map.offline.DesktopThumbnailFileManager
import com.jordankurtz.squawkscope.map.offline.DesktopThumbnailGenerator
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
            File(userHome, "Library/Caches/SquawkScope/tiles")
        osName.contains("win") -> {
            val localAppData =
                System.getenv("LOCALAPPDATA")
                    ?: File(userHome, "AppData/Local").path
            File(localAppData, "SquawkScope/tiles")
        }
        else -> {
            val xdgCacheHome =
                System.getenv("XDG_CACHE_HOME")
                    ?: File(userHome, ".cache").path
            File(xdgCacheHome, "SquawkScope/tiles")
        }
    }
}

internal fun desktopDbDir(): File {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    return when {
        osName.contains("mac") ->
            File(userHome, "Library/Caches/SquawkScope/db")
        osName.contains("win") -> {
            val localAppData =
                System.getenv("LOCALAPPDATA")
                    ?: File(userHome, "AppData/Local").path
            File(localAppData, "SquawkScope/db")
        }
        else -> {
            val xdgCacheHome =
                System.getenv("XDG_CACHE_HOME")
                    ?: File(userHome, ".cache").path
            File(xdgCacheHome, "SquawkScope/db")
        }
    }
}
