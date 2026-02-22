package com.darekbx.geoplanner.kmp.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Double,
    val timestamp: Long
)
