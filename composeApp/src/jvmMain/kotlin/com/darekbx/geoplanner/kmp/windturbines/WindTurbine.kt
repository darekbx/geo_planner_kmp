package com.darekbx.geoplanner.kmp.windturbines

import kotlinx.serialization.Serializable

@Serializable
data class OverpassWrapper(val elements: List<WindTurbine>)

@Serializable
data class WindTurbine(
    val type: String,
    val tags: Map<String, String>,
    val lat: Double? = null,
    val lon: Double? = null
) {
    fun hasLocation() = lon != null && lat != null

    fun description(): String = buildString {
        tags.get("height")?.let { append("Height: ${it}m") }
        tags.get("height:hub")?.let { append("Hub height: ${it}m") }
        tags.get("model")?.let { append("Model: $it") }
    }
}
