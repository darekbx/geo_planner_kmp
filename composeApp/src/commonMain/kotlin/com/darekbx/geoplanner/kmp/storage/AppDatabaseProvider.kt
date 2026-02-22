package com.darekbx.geoplanner.kmp.storage

import com.darekbx.geoplanner.kmp.db.AppDatabase

expect class AppDatabaseProvider {
    fun createDatabase(): AppDatabase
}