package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory(private val dbDir: File, private val dbName: String) {
    actual fun createDriver(): SqlDriver {
        dbDir.mkdirs()
        val dbFile = File(dbDir, dbName)
        // Check existence before opening — JdbcSqliteDriver creates the file on first connection,
        // so checking after open would always return true.
        val isNewDatabase = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNewDatabase) {
            TileCacheDatabase.Schema.create(driver)
        }
        return driver
    }
}
