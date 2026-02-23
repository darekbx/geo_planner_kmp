package com.darekbx.geoplanner.kmp.cloud

import com.darekbx.geoplanner.kmp.db.AppDatabaseQueries

class FirebaseSync(
    private val appDatabaseQueries: AppDatabaseQueries,
    private val firebaseClient: FirebaseClient
) {
    suspend fun start(progress: (Float) -> Unit, log: (String) -> Unit) {
        val steps = 8F

        // 1. Authenticate
        firebaseClient.authenticate()
            .also {
                log("Authenticated")
                progress(1F / steps)
            }

        // 2. Fetch places to visit
        val placesToVisit = firebaseClient.fetchPlacesToVisit()
            .also {
                log("Fetched ${it.size} places to visit")
                progress(2F / steps)
            }

        // 3. Delete places
        appDatabaseQueries.deleteAllPlaces()
            .also {
                log("Delete local places to visit")
                progress(3F / steps)
            }

        // 4. Add places
        placesToVisit.forEach { place ->
            appDatabaseQueries
                .insertPlace(place.label, place.latitude, place.longitude, now())
                .await()
        }.also {
            log("Added ${placesToVisit.size} places to visit")
            progress(4F / steps)
        }

        // 5. Fetch track ids
        val trackIds = firebaseClient.fetchTrackIds()
            .also {
                log("Fetched ${it.size} track ids")
                progress(5F / steps)
            }

        // 6. Fetch local track ids
        val localTrackIds = appDatabaseQueries.selectAllTrackIds().executeAsList()
            .also {
                log("Fetched ${it.size} local track ids")
                progress(6F / steps)
            }

        // 7. Get ids to fetch
        val trackIdsToFetch = trackIds - localTrackIds
        log("There's ${trackIdsToFetch.size} tracks to fetch")

        // 8. Fetch tracks by ids
        val tracks = if (localTrackIds.isEmpty()) {
            // 8a. Download all
            firebaseClient.fetchAllTracks()
                .also {
                    log("Fetched ${it.size} remote tracks")
                    progress(7F / steps)
                }
        } else {
            // 8b. Download new
            firebaseClient.fetchTracks(trackIdsToFetch)
                .also {
                    log("Fetched ${it.size} new remote tracks")
                    progress(7F / steps)
                }
        }

        // 9. Save tracks and points
        log("Saving tracks to database...")
        tracks
            .forEachIndexed { index, track ->
                appDatabaseQueries
                    .insertTrack(
                        track.localId.toLong(),
                        track.label,
                        track.startTimestamp,
                        track.endTimestamp,
                        track.distance
                    )
                    .await()
                    track.points.forEach { point ->
                    appDatabaseQueries
                        .insertPoint(
                            track.localId.toLong(),
                            point.timestamp,
                            point.latitude,
                            point.longitude,
                            point.speed,
                            point.altitude
                        )
                        .await()
                }
                progress(index / tracks.size.toFloat())
            }
            .also { log("Saved ${tracks.size} tracks and points") }

        log("Sync completed")
    }

    private fun now(): Long = System.currentTimeMillis()
}
