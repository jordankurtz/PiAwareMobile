package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.DatabaseDriverFactory
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import com.jordankurtz.piawaremobile.map.offline.SqlDelightOfflineTileStore
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class DatabaseModule {
    @Single
    actual fun provideTileCacheDatabase(contextWrapper: ContextWrapper): TileCacheDatabase {
        val dbDir = desktopDbDir()
        val driverFactory = DatabaseDriverFactory(dbDir, "piaware_mobile.db")
        return TileCacheDatabase(driverFactory.createDriver())
    }

    @Single
    actual fun provideOfflineTileStore(
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): OfflineTileStore = SqlDelightOfflineTileStore(database.tileCacheQueries, ioDispatcher)
}
