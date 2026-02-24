package com.darekbx.geoplanner.kmp.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.darekbx.geoplanner.kmp.cloud.FirebaseSync
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Track
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun Screen.Tracks(onTrackClick: (Long) -> Unit, onCleanSelection: () -> Unit) {
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
            is TracksUiState.TracksLoaded -> TracksList(state.tracks, onTrackClick, onCleanSelection) {
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
private fun BoxScope.InProgress() {
    CircularProgressIndicator(Modifier.align(Alignment.Center).size(64.dp))
}

@Composable
fun TracksList(
    tracks: List<Track>,
    onTrackClick: (Long) -> Unit,
    onCleanSelection: () -> Unit,
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
                        Modifier
                            .fillMaxWidth()
                            .clickable { onTrackClick(track.local_id) }
                            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
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
        Button(onClick = onCleanSelection, modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
            Text("Clean selection")
        }
        Button(onClick = onSynchronize, modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
            Text("Synchronize")
        }
    }
}
