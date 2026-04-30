package com.darekbx.geoplanner.kmp.windturbines

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class WindTurbinesProvider(
    private val httpClient: HttpClient
) {

    suspend fun fetch(): List<WindTurbine> {
        val response = httpClient.get(urlString = OSM_QUERY)
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseData = response.body<OverpassWrapper>()
                responseData.elements
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    companion object {
        private const val OSM_QUERY =
            "https://overpass-api.de/api/interpreter?data=%5Bout%3Ajson%5D%5Btimeout%3A25%5D%3B%0Anwr%5B%22generator%3Amethod%22%3D%22wind_turbine%22%5D%2851.51219196266224%2C18.468017578125004%2C52.902305628635254%2C23.9007568359375%29%3B%0Aout%20geom%3B"
    }
}
