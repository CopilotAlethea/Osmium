package org.dazai.osmium.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home")
        val databaseDir = File(userHome, ".osmium")
        if (!databaseDir.exists()) {
            databaseDir.mkdirs()
        }
        val databaseFile = File(databaseDir, "osmium.db")
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        
        val version = getVersion(driver)
        if (version == 0L) {
            OsmiumDatabase.Schema.create(driver)
            setVersion(driver, OsmiumDatabase.Schema.version)
        } else {
            // Comprehensive Hotfix for missing columns in existing tables
            // These run regardless of version to fix inconsistent development databases
            
            // 1. AnnotationEntity hotfixes
            val annotationCols = listOf(
                "href TEXT NOT NULL DEFAULT ''",
                "mediaType TEXT NOT NULL DEFAULT ''",
                "locations TEXT NOT NULL DEFAULT '{}'",
                "text TEXT NOT NULL DEFAULT '{}'",
                "title TEXT",
                "totalProgression REAL NOT NULL DEFAULT 0.0",
                "style TEXT",
                "chapter TEXT",
                "chapterProgression REAL"
            )
            
            annotationCols.forEach { colDef ->
                try {
                    val colName = colDef.substringBefore(" ")
                    driver.execute(null, "ALTER TABLE AnnotationEntity ADD COLUMN $colDef;", 0)
                } catch (e: Exception) {
                    // Column likely already exists
                }
            }

            // 2. BookEntity hotfixes
            val bookCols = listOf(
                "titlePinyin TEXT NOT NULL DEFAULT ''",
                "titleInitials TEXT NOT NULL DEFAULT ''"
            )
            
            bookCols.forEach { colDef ->
                try {
                    driver.execute(null, "ALTER TABLE BookEntity ADD COLUMN $colDef;", 0)
                } catch (e: Exception) {}
            }
            
            if (version < OsmiumDatabase.Schema.version) {
                setVersion(driver, OsmiumDatabase.Schema.version)
            }
        }
        return driver
    }

    private fun getVersion(driver: SqlDriver): Long {
        val query = driver.executeQuery(null, "PRAGMA user_version;", { cursor ->
            val result = if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L
            QueryResult.Value(result)
        }, 0)
        return query.value
    }

    private fun setVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version;", 0)
    }
}

