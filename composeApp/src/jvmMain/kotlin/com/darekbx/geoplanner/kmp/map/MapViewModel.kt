package com.darekbx.geoplanner.kmp.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import cafe.adriel.voyager.core.model.ScreenModel
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Place
import com.darekbx.geoplanner.kmp.map.providers.BaseTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

class MapViewModel(
    private val tileProvider: BaseTileProvider,
    private val appDatabaseQueries: AppDatabaseQueries
) : ScreenModel {

    val maxLevel = 16
    val minLevel = 12
    val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)

    val state =
        MapState(levelCount = maxLevel + 1, mapSize, mapSize, workerCount = 16) {
            minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
            val (x, y) = latLngToNormalized(52.2297, 21.0122)
            scroll(x, y)
        }.apply {
            addLayer(tileProvider.create())
            scale = 0.0

            onPathClick { id, x, y ->

                addCallout(
                    id, x, y,
                    absoluteOffset = DpOffset(0.dp, (-10).dp),
                ) {
                    Callout(x, y, title = "Click on $id")
                }
            }

            addPath(
                id = "id",
                color = Color.Red,
                //fillColor = Color.Green.copy(alpha = .6f),
                //pattern = listOf(PatternItem.Dash(8.dp), PatternItem.Gap(4.dp)),
                clickable = true
            ) {
                val (x1, y1) = latLngToNormalized(52.2297, 21.0122)
                addPoint(x1, y1)
                val (x2, y2) = latLngToNormalized(52.2217, 21.0102)
                addPoint(x2, y2)
                val (x3, y3) = latLngToNormalized(52.2247, 21.0212)
                addPoint(x3, y3)
                val (x4, y4) = latLngToNormalized(52.2047, 21.0212)
                addPoint(x4, y4)
            }
        }

    suspend fun addPlacesToVisit() {
        withContext(Dispatchers.IO) {
            val places = appDatabaseQueries.selectAllPlaces().execute { cursor ->
                val result = mutableListOf<Place>()
                while (cursor.next().value) result.add(mapPlace(cursor))
                QueryResult.Value(result)
            }.await()
            places.forEach { place ->
                val (x, y) = latLngToNormalized(place.latitude, place.longitude)
                state.addMarker("${place.id}", x, y) { PlaceDot() }
            }
        }
    }

    suspend fun addTracks() {
        withContext(Dispatchers.IO) {
            //appDatabaseQueries.selectAllPlaces().execute().await()
        }
    }

    suspend fun zoomIn() {
        val newScale = (state.scale * ZOOM_STEP).coerceAtMost(state.maxScale)
        zoomInOut(newScale)
    }

    suspend fun zoomOut() {
        val newScale = (state.scale / ZOOM_STEP).coerceAtMost(state.maxScale)
        zoomInOut(newScale)
    }

    suspend fun zoomInOut(scale: Double) {
        state.scrollTo(
            state.centroidX,
            state.centroidY,
            destScale = scale
        )
    }

    private fun mapPlace(cursor: SqlCursor) =
        Place(
            id = cursor.getLong(0)!!,
            label = cursor.getString(1)!!,
            latitude = cursor.getDouble(2)!!,
            longitude = cursor.getDouble(3)!!,
            timestamp = cursor.getLong(4)!!
        )

    companion object {
        private const val ZOOM_STEP = 1.5
    }
}

