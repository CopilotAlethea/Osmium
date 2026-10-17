package org.dazai.osmium.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.dazai.osmium.util.PlatformContext

actual class DatabaseDriverFactory(private val context: Context) {
    init {
        PlatformContext.context = context.applicationContext
    }

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(OsmiumDatabase.Schema, context, "osmium.db")
    }
}

