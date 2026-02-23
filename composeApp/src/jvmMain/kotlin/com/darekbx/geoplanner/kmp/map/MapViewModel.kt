package com.darekbx.geoplanner.kmp.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import cafe.adriel.voyager.core.model.ScreenModel
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Place
import com.darekbx.geoplanner.kmp.db.Point
import com.darekbx.geoplanner.kmp.db.Track
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
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

data class Highlighted(val track: Track, val points: List<Point>)

class MapViewModel(
    private val tileProvider: BaseTileProvider,
    private val appDatabaseQueries: AppDatabaseQueries
) : ScreenModel {

    private var highlightId: String? = null
    var highlighedTrack = mutableStateOf<Highlighted?>(null)

    val state =
        MapState(levelCount = maxLevel + 1, mapSize, mapSize, workerCount = 16) {
            minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
            val (x, y) = latLngToPoint(52.2297, 21.0122)
            scroll(x, y)
        }.apply {
            addLayer(tileProvider.create())
            scale = 0.0

            onPathClick { id, x, y ->
                addCallout(
                    id, x, y,
                    absoluteOffset = DpOffset(0.dp, (-10).dp),
                ) {
                    //Callout(x, y, title = "Click on $id")
                }
            }
        }

    fun removeHighlight() {
        highlightId?.let { state.removePath(it) }
        highlighedTrack.value = null
        highlightId = null
    }

    suspend fun highlightTrack(localId: Long) {
        removeHighlight()

        // Check selected track
        val id = "selected-${localId}"
        if (id == highlightId) {
            removeHighlight()
            return
        }

        // Select track
        val track = appDatabaseQueries.selectTrack(localId).executeAsOne()
        val points = appDatabaseQueries.selectPoints(localId).executeAsList()
        state.addPath(id, color = Color.Blue, width = 4.dp, zIndex = 1F, clickable = true) {
            val pointsTransformed = points
                .map { point -> latLngToPoint(point.latitude, point.longitude) }
            addPoints(pointsTransformed)
        }

        // Move to track
        val firstPoint = points.first()
        val (x, y) = latLngToPoint(firstPoint.latitude, firstPoint.longitude)
        state.scrollTo(x, y)

        // Mark highlight
        highlighedTrack.value = Highlighted(track, points)
        highlightId = id
    }

    suspend fun addPlacesToVisit() {
        withContext(Dispatchers.IO) {
            val places = appDatabaseQueries.selectAllPlaces().execute { cursor ->
                val result = mutableListOf<Place>()
                while (cursor.next().value) result.add(mapPlace(cursor))
                QueryResult.Value(result)
            }.await()
            places.forEach { place ->
                val (x, y) = latLngToPoint(place.latitude, place.longitude)
                state.addMarker("${place.id}", x, y) { PlaceDot() }
            }
        }
    }

    suspend fun addTracks() {
        withContext(Dispatchers.IO) {
            val tracks = appDatabaseQueries.selectTrackWithPoints().executeAsList()
            val groupped = tracks.groupBy { it.local_id }
            groupped.forEach { (trackId, points) ->
                state.addPath("$trackId", color = Color.Red, width = 1.dp, clickable = true) {
                    val pointsTransformed = points
                        .sortedBy { it.id }
                        .map { point -> latLngToPoint(point.latitude, point.longitude) }
                    addPoints(pointsTransformed)
                }
            }
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
        private const val maxLevel = 16
        private const val minLevel = 12
        private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    }
}

