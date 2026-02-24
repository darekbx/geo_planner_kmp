package com.darekbx.geoplanner.kmp.map

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.time.Instant
private const val EARTH_RADIUS_METERS = 6371000.0

fun distanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)

    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)

    val a = sin(dLat / 2).pow(2) +
            cos(rLat1) * cos(rLat2) *
            sin(dLng / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_METERS * c
}

fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

fun latLngToPoint(lat: Double, lng: Double): Pair<Double, Double> {
    val x = (lng + 180.0) / 360.0
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0
    return x to y
}

fun pointToLatLng(x: Double, y: Double): Pair<Double, Double> {
    val lng = x * 360.0 - 180.0
    val n = PI - 2.0 * PI * y
    val lat = Math.toDegrees(atan(sinh(n)))
    return lat to lng
}
object SpeedUtils {
    fun msToKm(ms: Double): Float = ms.toFloat() * 3.6F
}

fun formatTimestampToDate(
    timestampMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val instant = Instant.fromEpochMilliseconds(timestampMillis)
    val localDateTime = instant.toLocalDateTime(timeZone)
    return "${localDateTime.year.toString().padStart(4, '0')}-" +
            "${localDateTime.month.number.toString().padStart(2, '0')}-" +
            localDateTime.day.toString().padStart(2, '0')
}

fun formatTimestampToDateTime(
    timestampMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val instant = Instant.fromEpochMilliseconds(timestampMillis)
    val localDateTime = instant.toLocalDateTime(timeZone)
    return "${localDateTime.year.toString().padStart(4, '0')}-" +
            "${localDateTime.month.number.toString().padStart(2, '0')}-" +
            "${localDateTime.day.toString().padStart(2, '0')} " +
            "${localDateTime.hour.toString().padStart(2, '0')}:" +
            "${localDateTime.minute.toString().padStart(2, '0')}:" +
            "${localDateTime.second.toString().padStart(2, '0')}"
}
