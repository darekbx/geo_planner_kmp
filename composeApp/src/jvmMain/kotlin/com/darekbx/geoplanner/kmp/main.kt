package com.darekbx.geoplanner.kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.darekbx.geoplanner.kmp.di.appModule
import com.darekbx.geoplanner.kmp.di.httpModule
import org.koin.core.context.startKoin

/**
 * GeoPlanner TODO:
 *  - use local database to store data from the cloud (sqldelight)
 *  - synchronize date this clound, add only new records
 *  - display stored routes, only when there are points
 *  - display all routes in list on the right (mark routes with points)
 *  - add ability to plan route and export to GPX file
 *  - display places to visit
 *  - display wind turbines
*/
fun main() {
    startKoin {
        modules(appModule, httpModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(placement = WindowPlacement.Maximized),
            title = "Geo Planner",
        ) {
            App()
        }
    }
}