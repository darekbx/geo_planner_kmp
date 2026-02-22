package com.darekbx.geoplanner.kmp.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.darekbx.geoplanner.kmp.db.AppDatabase
import java.io.File

actual class AppDatabaseProvider {
    actual fun createDatabase(): AppDatabase {
        val databaseFile = File("app.db")
        //databaseFile.delete()

        val isNewDatabase = !databaseFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")

        if (isNewDatabase) {
            AppDatabase.Schema.create(driver)
        }

        return AppDatabase(driver)
    }
}
