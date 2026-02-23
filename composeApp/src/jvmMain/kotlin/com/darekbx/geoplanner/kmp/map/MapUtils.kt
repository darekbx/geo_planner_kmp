package com.darekbx.geoplanner.kmp.map

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.time.Instant

fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

fun latLngToPoint(lat: Double, lng: Double): Pair<Double, Double> {
    val x = (lng + 180.0) / 360.0
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0
    return x to y
}

object SpeedUtils {

    fun msToKm(ms: Float) = ms * 3.6F

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
