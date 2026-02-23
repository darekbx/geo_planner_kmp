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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
                        zoomOut = { scope.launch { screenModel.zoomOut() } }
                    )
                }

                highlight?.let {
                    TrackInfo(it)
                }
            }
            Tracks { clickedTrack ->
               //TODO: show chart of speed and altitude, also start/end timestam, time diff

                scope.launch {
                    screenModel.highlightTrack(clickedTrack)
                }
            }
        }
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
