package com.darekbx.geoplanner.kmp.di

import com.darekbx.geoplanner.kmp.cloud.FirebaseClient
import com.darekbx.geoplanner.kmp.cloud.FirebaseSession
import com.darekbx.geoplanner.kmp.cloud.FirebaseSync
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.map.providers.BaseTileProvider
import com.darekbx.geoplanner.kmp.map.providers.OsmTileProvider
import com.darekbx.geoplanner.kmp.storage.AppDatabaseProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    factory<BaseTileProvider> { OsmTileProvider() }
    single<AppDatabaseQueries> { AppDatabaseProvider().createDatabase().appDatabaseQueries }
    factory { FirebaseSync(get(), get()) }
    factory { FirebaseClient(get(), get()) }

    single { FirebaseSession() }
}

val httpModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }

            expectSuccess = true
        }
    }
}