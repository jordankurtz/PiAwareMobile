package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(TileCacheDatabase.Schema, "tile_cache.db")
}
