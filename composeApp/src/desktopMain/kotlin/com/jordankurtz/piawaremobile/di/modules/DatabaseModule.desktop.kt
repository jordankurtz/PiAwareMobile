package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.map.cache.DatabaseDriverFactory
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
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
}
