package com.darekbx.geoplanner.kmp.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class NormalizedPoint(val x: Double, val y: Double)

fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

fun latLngToNormalized(lat: Double, lng: Double): NormalizedPoint {
    val x = (lng + 180.0) / 360.0

    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0

    return NormalizedPoint(x, y)
}
