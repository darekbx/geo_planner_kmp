package com.darekbx.geoplanner.kmp.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import cafe.adriel.voyager.core.model.ScreenModel
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Place
import com.darekbx.geoplanner.kmp.db.Point
import com.darekbx.geoplanner.kmp.db.Track
import com.darekbx.geoplanner.kmp.map.providers.BaseTileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.updatePath
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


data class Highlighted(val track: Track, val points: List<Point>)
data class RoutePoint(val pointId: String, val point: Pair<Double, Double>)
data class RoutePath(val pathId: String)
data class RouteInfo(val points: Int, val distance: Double)

class MapViewModel(
    private val tileProvider: BaseTileProvider,
    private val appDatabaseQueries: AppDatabaseQueries,
    private val gpxCreator: GPXCreator
) : ScreenModel {

    private var highlightId: String? = null
    private var routePoints = mutableListOf<RoutePoint>()
    private var routePaths = mutableListOf<RoutePath>()
    private var trackPathIds = mutableListOf<String>()
    private var isLocked = false

    var highlighedTrack = mutableStateOf<Highlighted?>(null)
    var isRoutePlanning = mutableStateOf(false)
    var routeInfo = mutableStateOf<RouteInfo?>(null)

    val state =
        MapState(levelCount = maxLevel + 1, mapSize, mapSize, workerCount = 16) {
            minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
            val (x, y) = latLngToPoint(52.149, 21.027)
            scroll(x, y)
        }.apply {
            addLayer(tileProvider.create())
            scale = 0.1

            // Highlight path on click
            onPathClick { id, _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    highlightTrack(id.toLong())
                }
            }

            // Add route point on tap
            onTap { x, y -> createRoute(x, y)  }
        }

    fun createGPX() {
        val gpxXml = gpxCreator.createXml(routePoints
            .map { pointToLatLng(it.point.first, it.point.second) })

        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save file"
            selectedFile = File("route.gpx")
            fileFilter = FileNameExtensionFilter("GPX file", "gpx")
        }

        val result = fileChooser.showSaveDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            file.writeText(gpxXml)
        }
    }

    fun undoPoint() {
        if (routePoints.isNotEmpty()) {
            val lastPoint = routePoints.removeLast()
            val lastPath = routePaths.removeLast()
            state.removeMarker(lastPoint.pointId)
            state.removePath(lastPath.pathId)
            updateRouteInfo()
        } else {
            isRoutePlanning.value = false
        }
    }

    fun resetRoutePlanning() {
        isRoutePlanning.value = false

        routePoints.forEach { state.removeMarker(it.pointId) }
        routePaths.forEach { state.removePath(it.pathId) }

        routePoints.clear()
        routePaths.clear()
    }

    fun removeHighlight() {
        highlightId?.let { state.removePath(it) }
        highlighedTrack.value = null
        highlightId = null
    }

    suspend fun highlightTrack(localId: Long) {
        if (isLocked) return

        removeHighlight()

        // Select track
        val id = "selected-${localId}"
        val track = appDatabaseQueries.selectTrack(localId).executeAsOne()
        val points = appDatabaseQueries.selectPoints(localId).executeAsList()
        state.addPath(id, color = Color.Blue, width = 4.dp, zIndex = 1F, clickable = false) {
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
                trackPathIds.add("$trackId")
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

    private fun MapState.createRoute(x: Double, y: Double) {
        isRoutePlanning.value = true

        routePoints.add(RoutePoint("path-${routePoints.size + 1}", x to y))
        addMarker("path-${routePoints.size}", x, y) { PlaceDot() }

        if (routePoints.size > 1) {
            addPath("line-${routePoints.size}", color = Color.Black, width = 2.dp, cap = Cap.Round) {
                val p0 = routePoints[routePoints.size - 2]
                addPoint(p0.point.first, p0.point.second)
                addPoint(x, y)
            }
            routePaths.add(RoutePath("line-${routePoints.size}"))
        }

        updateRouteInfo()
    }

    private fun updateRouteInfo() {
        val latLng = routePoints
            .map { pointToLatLng(it.point.first, it.point.second) }

        var sumDistance = 0.0

        if (routePoints.size > 1) {
            var (firstLat, firstLng) = latLng.first()
            latLng.drop(1).forEach { point ->
                val (lat, lng) = point
                sumDistance += distanceMeters(firstLat, firstLng, lat, lng)
                firstLat = lat
                firstLng = lng
            }
        }

        routeInfo.value = RouteInfo(routePoints.size, sumDistance)
    }

    fun disableTrackClick() {
        isLocked = true
        trackPathIds.forEach {
            state.updatePath(it, clickable = false)
        }
    }

    fun enableTrackClick() {
        isLocked = false
        trackPathIds.forEach {
            state.updatePath(it, clickable = true)
        }
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
        private const val minLevel = 11
        private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    }
}

