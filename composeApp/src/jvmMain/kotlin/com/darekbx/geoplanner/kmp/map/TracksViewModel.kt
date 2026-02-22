package com.darekbx.geoplanner.kmp.map

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.darekbx.geoplanner.kmp.cloud.FirebaseSync
import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries
import com.darekbx.geoplanner.kmp.db.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TracksUiState {
    data object Idle : TracksUiState()
    data class Error(val e: Throwable) : TracksUiState()
    data object InProgress : TracksUiState()
    data object Synchronizing : TracksUiState()
    data class TracksLoaded(val tracks: List<Track>) : TracksUiState()
}

class TracksViewModel(
    private val appDatabaseQueries: AppDatabaseQueries,
    private val firebaseSync: FirebaseSync
) : ScreenModel {

    var uiState = mutableStateOf<TracksUiState>(TracksUiState.Idle)
    var progress = mutableStateOf(0F)
    var logs = mutableStateListOf<String>()

    fun loadTracks() {
        screenModelScope.launch {
            uiState.value = TracksUiState.InProgress
            try {
                val tracks = appDatabaseQueries.selectAllTracks().executeAsList()
                    .sortedByDescending { it.start_timestamp }
                uiState.value = TracksUiState.TracksLoaded(tracks)
            } catch (e: Exception) {
                e.printStackTrace()
                uiState.value = TracksUiState.Error(e)
            }
        }
    }

    fun synchronize() {
        screenModelScope.launch {
            uiState.value = TracksUiState.Synchronizing
            withContext(Dispatchers.IO) {
                try {
                    // Synchronize
                    firebaseSync.start(
                        progress = { progress.value = it },
                        log = { logs.add(it) }
                    )

                    // Load
                    loadTracks()
                } catch (e: Exception) {
                    logs.add(e.toString())
                    delay(5000L)
                    uiState.value = TracksUiState.Idle
                }
            }
        }
    }
}