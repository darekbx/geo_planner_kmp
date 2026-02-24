package com.darekbx.geoplanner.kmp.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.darekbx.geoplanner.kmp.map.providers.BaseTileProvider
import org.koin.compose.koinInject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI

object GeoTrackerMapScreen : Screen {

    private fun readResolve(): Any = GeoTrackerMapScreen

    @Composable
    override fun Content() {
        val tileProvider: BaseTileProvider = koinInject()
        val appDatabaseQueries: AppDatabaseQueries = koinInject()
        val screenModel = rememberScreenModel { MapViewModel(tileProvider, appDatabaseQueries) }
        val scope = rememberCoroutineScope()

        val highlight by screenModel.highlighedTrack
        val isRoutePlanning by screenModel.isRoutePlanning
        val routeInfo by screenModel.routeInfo

        LaunchedEffect(Unit) {
            screenModel.addPlacesToVisit()
            screenModel.addTracks()
        }

        Row(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().weight(1f)) {
                Box(Modifier.fillMaxSize().weight(1F)) {

                    MapUI(Modifier.fillMaxSize(), state = screenModel.state)

                    MapZoomButtons(
                        zoomIn = { scope.launch { screenModel.zoomIn() } },
                        zoomOut = { scope.launch { screenModel.zoomOut() } },
                        onLock = { locked ->
                            if (locked) screenModel.disableTrackClick()
                            else screenModel.enableTrackClick()
                        }
                    )

                    if (isRoutePlanning) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(Modifier.width(IntrinsicSize.Max)) {
                                Text(
                                    text = "${format((routeInfo?.distance ?: 0.0) / 1000)}km (${routeInfo?.points} points)",
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { screenModel.resetRoutePlanning() }) { Text("Reset") }
                                    Button(onClick = { screenModel.undoPoint() }) { Text("Undo") }
                                }
                                Button(
                                    onClick = { /* TODO */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Export to GPX") }
                            }
                        }
                    }
                }

                highlight?.let { TrackInfo(it) }
            }
            Tracks(
                onTrackClick = { clickedTrack ->
                    scope.launch {
                        screenModel.highlightTrack(clickedTrack)
                    }
                },
                onCleanSelection = { screenModel.removeHighlight() }
            )
        }
    }

    private fun format(value: Double): String = "%.2f".format(value)
}

@Composable
fun BoxScope.MapZoomButtons(zoomIn: () -> Unit = { }, zoomOut: () -> Unit = { }, onLock: (Boolean) -> Unit = { }) {
    var lock by remember { mutableStateOf(false) }
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
        Button(
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            onClick = {
                lock = !lock
                onLock(lock)
            }
        ) {
            val icon = if (lock) Icons.Default.Lock else Icons.Default.LockOpen
            Icon(icon, modifier = Modifier.size(28.dp), contentDescription = null)
        }
    }
}

@Composable
fun PlaceDot() {
    Box(Modifier.size(5.dp).background(Color.Black, CircleShape))
}

@Preview
@Composable
fun MapZoomButtonsPreview() {
    Box { MapZoomButtons() }
}
