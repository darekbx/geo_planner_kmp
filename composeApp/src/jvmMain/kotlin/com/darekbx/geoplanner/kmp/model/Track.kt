package com.darekbx.geoplanner.kmp.model

data class Track(
    val localId: Int,
    val label: String,
    val distance: Double,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val points: List<TrackPoint>
)
