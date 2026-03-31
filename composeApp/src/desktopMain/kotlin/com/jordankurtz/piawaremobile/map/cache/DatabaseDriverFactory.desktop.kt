package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory(private val dbDir: File) {
    actual fun createDriver(): SqlDriver {
        dbDir.mkdirs()
        val dbPath = File(dbDir, "tile_cache.db").absolutePath
        return JdbcSqliteDriver("jdbc:sqlite:$dbPath").also { driver ->
            TileCacheDatabase.Schema.create(driver)
        }
    }
}
