package com.darekbx.geoplanner.kmp.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.darekbx.geoplanner.kmp.map.providers.BaseTileProvider
import org.koin.compose.koinInject
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darekbx.geoplanner.kmp.cloud.FirebaseSync
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Track
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import ovh.plrapps.mapcompose.ui.MapUI
import kotlin.time.Instant

object GeoTrackerMapScreen : Screen {

    private fun readResolve(): Any = GeoTrackerMapScreen

    @Composable
    override fun Content() {
        val tileProvider: BaseTileProvider = koinInject()
        val appDatabaseQueries: AppDatabaseQueries = koinInject()
        val screenModel = rememberScreenModel { MapViewModel(tileProvider, appDatabaseQueries) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            screenModel.addPlacesToVisit()
            screenModel.addTracks()
        }

        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().weight(1F)) {

                MapUI(Modifier.fillMaxSize(), state = screenModel.state)

                MapZoomButtons(
                    zoomIn = { scope.launch { screenModel.zoomIn() } },
                    zoomOut = { scope.launch { screenModel.zoomOut() } }
                )
            }
            Tracks()
        }
    }

    @Composable
    private fun Tracks() {
        val appDatabaseQueries: AppDatabaseQueries = koinInject()
        val firebaseSync: FirebaseSync = koinInject()
        val tracksViewModel = rememberScreenModel { TracksViewModel(appDatabaseQueries, firebaseSync) }
        val uiState by tracksViewModel.uiState

        LaunchedEffect(Unit) {
            tracksViewModel.loadTracks()
        }

        Box(Modifier.width(208.dp).fillMaxHeight().background(Color.White), contentAlignment = Alignment.TopStart) {
            when (val state = uiState) {
                TracksUiState.Idle -> {}
                TracksUiState.InProgress -> InProgress()
                TracksUiState.Synchronizing -> SynchronizeStatus(tracksViewModel)
                is TracksUiState.Error -> ErrorView(state.e)
                is TracksUiState.TracksLoaded -> TracksList(state.tracks) {
                    tracksViewModel.synchronize()
                }
            }
        }
    }

    @Composable
    private fun SynchronizeStatus(tracksViewModel: TracksViewModel) {
        val logs = tracksViewModel.logs
        val progress by tracksViewModel.progress
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                drawStopIndicator = { },
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                items(logs) { log ->
                    Text(
                        text = log,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }


    @Composable
    private fun ErrorView(e: Throwable) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "$e", color = MaterialTheme.colors.error)
        }
    }

    @Composable
    private fun TracksList(
        tracks: List<Track>,
        onSynchronize: () -> Unit
    ) {
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5F)
                    .padding(top = 2.dp, bottom = 2.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                scrollState.scrollBy(-delta)
                            }
                        },
                    ),
                state = scrollState
            ) {
                items(tracks) { track ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Text(
                                    formatTimestampToDate(track.start_timestamp),
                                    style = MaterialTheme.typography.caption,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 6.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${((track.distance ?: 0.0) / 1000.0).toInt()}km",
                                    modifier = Modifier.width(50.dp).background(Color.LightGray.copy(alpha = 0.3F)),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.caption,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    fontSize = 11.sp,
                                    lineHeight = 6.sp
                                )
                            }
                            Text(
                                "ID: ${track.local_id}",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 6.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = (0.5).dp)
                    }
                }
            }
            HorizontalDivider(Modifier.fillMaxWidth())
            Button(onClick = onSynchronize, modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
                Text("Synchronize")
            }
        }
    }

    @Composable
    private fun BoxScope.InProgress() {
        CircularProgressIndicator(Modifier.align(Alignment.Center).size(64.dp))
    }

    private fun formatTimestampToDate(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val instant = Instant.fromEpochMilliseconds(timestampMillis)
        val localDateTime = instant.toLocalDateTime(timeZone)
        return "${localDateTime.year.toString().padStart(4, '0')}-" +
                "${localDateTime.month.number.toString().padStart(2, '0')}-" +
                localDateTime.day.toString().padStart(2, '0')
    }
}

@Composable
fun BoxScope.MapZoomButtons(zoomIn: () -> Unit = { }, zoomOut: () -> Unit = { }) {
    Column(
        Modifier.padding(end = 32.dp, bottom = 32.dp).align(Alignment.BottomEnd),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            onClick = zoomIn
        ) {
            Icon(Icons.Default.ZoomIn, modifier = Modifier.size(28.dp), contentDescription = null)
        }
        Button(
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            onClick = zoomOut
        ) {
            Icon(Icons.Default.ZoomOut, modifier = Modifier.size(28.dp), contentDescription = null)
        }
    }
}

@Composable
fun PlaceDot() {
    Box(Modifier.size(6.dp).background(Color.Black, CircleShape))
}

@Preview
@Composable
fun MapZoomButtonsPreview() {
    Box { MapZoomButtons() }
}

@Composable
fun Callout(
    x: Double, y: Double,
    title: String,
) {
    Surface(
        Modifier.padding(10.dp),
        shape = RoundedCornerShape(5.dp),
        tonalElevation = 10.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "position ${x.format()} , ${y.format()}",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

private fun Double.format(): String {
    return (round(this * 100) / 100).toString()
}
