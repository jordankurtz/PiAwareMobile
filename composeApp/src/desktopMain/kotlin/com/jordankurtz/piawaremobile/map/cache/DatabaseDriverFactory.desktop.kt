package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory(private val dbDir: File, private val dbName: String) {
    actual fun createDriver(): SqlDriver {
        dbDir.mkdirs()
        val dbFile = File(dbDir, dbName)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val currentVersion =
            driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version",
                mapper = { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0)!!)
                },
                parameters = 0,
            ).value
        if (currentVersion == 0L) {
            TileCacheDatabase.Schema.create(driver)
            driver.execute(null, "PRAGMA user_version = ${TileCacheDatabase.Schema.version}", 0)
        } else if (currentVersion < TileCacheDatabase.Schema.version) {
            TileCacheDatabase.Schema.migrate(driver, currentVersion, TileCacheDatabase.Schema.version)
            driver.execute(null, "PRAGMA user_version = ${TileCacheDatabase.Schema.version}", 0)
        }
        return driver
    }
}
