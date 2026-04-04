package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
expect class DatabaseModule() {
    @Single
    fun provideTileCacheDatabase(contextWrapper: ContextWrapper): TileCacheDatabase
}
