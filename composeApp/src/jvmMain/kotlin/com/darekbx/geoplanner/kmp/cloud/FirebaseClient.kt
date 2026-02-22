package com.darekbx.geoplanner.kmp.cloud

import com.darekbx.geoplanner.kmp.CloudConfiguration
import com.darekbx.geoplanner.kmp.cloud.dto.ArrayValue
import com.darekbx.geoplanner.kmp.cloud.dto.ArrayValues
import com.darekbx.geoplanner.kmp.cloud.dto.AuthCredentials
import com.darekbx.geoplanner.kmp.cloud.dto.AuthResponse
import com.darekbx.geoplanner.kmp.cloud.dto.FieldFilter
import com.darekbx.geoplanner.kmp.cloud.dto.FieldPath
import com.darekbx.geoplanner.kmp.cloud.dto.From
import com.darekbx.geoplanner.kmp.cloud.dto.LongValue
import com.darekbx.geoplanner.kmp.cloud.dto.PlacesToVisitFields
import com.darekbx.geoplanner.kmp.cloud.dto.PlacesToVisitResponse
import com.darekbx.geoplanner.kmp.cloud.dto.RunQueryRequest
import com.darekbx.geoplanner.kmp.cloud.dto.RunQueryResponse
import com.darekbx.geoplanner.kmp.cloud.dto.StringValue
import com.darekbx.geoplanner.kmp.cloud.dto.StructuredQuery
import com.darekbx.geoplanner.kmp.cloud.dto.TrackFields
import com.darekbx.geoplanner.kmp.cloud.dto.TrackIdsResponse
import com.darekbx.geoplanner.kmp.cloud.dto.Where
import com.darekbx.geoplanner.kmp.model.PlaceToVisit
import com.darekbx.geoplanner.kmp.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class FirebaseClient(
    private val httpClient: HttpClient,
    private val session: FirebaseSession
) {

    suspend fun authenticate() {
        val response = httpClient.post(urlString = "$AUTH_ENDPOINT?key=${CloudConfiguration.API_KEY}") {
            contentType(ContentType.Application.Json)
            setBody(AuthCredentials(CloudConfiguration.EMAIL, CloudConfiguration.PASSWORD, true))
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val authResponse = response.body<AuthResponse>()
                session.setAuthResponse(authResponse)
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    suspend fun fetchTrackIds(
        ids: List<Long> = listOf(),
        pageToken: String? = null
    ): List<Long> {
        val url = pageToken
            ?.let { "$endpoint/documents/$TRACK_IDS?pageToken=$pageToken" }
            ?: "$endpoint/documents/$TRACK_IDS"
        val response = httpClient.get(urlString = url) { addAuthHeader() }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseData = response.body<TrackIdsResponse>()
                val pageData = responseData.documents.map { it.fields.id.value.toLong() }
                if (responseData.nextPageToken != null) {
                    fetchTrackIds(ids + pageData, responseData.nextPageToken)
                } else {
                    pageData
                }
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    suspend fun fetchPlacesToVisit(
        data: List<PlaceToVisit> = listOf(),
        pageToken: String? = null
    ): List<PlaceToVisit> {
        val url = pageToken
            ?.let { "$endpoint/documents/$PLACES_TO_VISIT?pageToken=$pageToken" }
            ?: "$endpoint/documents/$PLACES_TO_VISIT"
        val response = httpClient.get(urlString = url) { addAuthHeader() }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseData = response.body<PlacesToVisitResponse>()
                val pageData = responseData.documents.map { it.fields.toPlaceToVisit() }
                if (responseData.nextPageToken != null) {
                    fetchPlacesToVisit(data + pageData, responseData.nextPageToken)
                } else {
                    pageData
                }
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    suspend fun fetchAllTracks(): List<Track> {
        val url = "$endpoint/documents:runQuery"
        val response = httpClient.post(urlString = url) {
            setBody(
                RunQueryRequest(
                    structuredQuery = StructuredQuery(
                        from = listOf(From(TRACK)),
                        where = Where(
                            fieldFilter = FieldFilter(
                                field = FieldPath("points"),
                                op = "NOT_EQUAL",
                                value = StringValue("[]")
                            )
                        )
                    )
                )
            )
            contentType(ContentType.Application.Json)
            addAuthHeader()
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseData = response.body<List<RunQueryResponse>>()
                val documents = responseData.mapNotNull { it.document }
                documents.map { it.fields.toTrack() }
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    suspend fun fetchTracks(ids: List<Long>): List<Track> {
        val url = "$endpoint/documents:runQuery"
        val response = httpClient.post(urlString = url) {
            setBody(
                RunQueryRequest(
                    structuredQuery = StructuredQuery(
                        from = listOf(From(TRACK)),
                        where = Where(
                            fieldFilter = FieldFilter(
                                field = FieldPath(fieldPath = "local_id"),
                                op = "IN",
                                value = ArrayValue(
                                    value = ArrayValues(values = ids.map { LongValue(it) })
                                )
                            )
                        )
                    )
                )
            )
            contentType(ContentType.Application.Json)
            addAuthHeader()
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseData = response.body<List<RunQueryResponse>>()
                val documents = responseData.mapNotNull { it.document }
                documents
                    .map { it.fields.toTrack() }
                    .filter { it.points.isNotEmpty() }
            }

            else -> throw Exception("HTTP ${response.status.value}")
        }
    }

    private fun PlacesToVisitFields.toPlaceToVisit(): PlaceToVisit =
        PlaceToVisit(label.value, latitude.value, longitude.value)

    private fun TrackFields.toTrack() =
        Track(
            localId = localId.value,
            label = label.value,
            distance = distance.value,
            startTimestamp = startTimestamp.value,
            endTimestamp = endTimestamp.value,
            points = Json.decodeFromString(points.value)
        )

    private fun HttpMessageBuilder.addAuthHeader() {
        header(HttpHeaders.Authorization, "Bearer ${session.getToken()}")
    }

    private val endpoint: String by lazy {
        "${PROJECT_ENDPOINT}/${CloudConfiguration.PROJECT_ID}/databases/(default)"
    }

    companion object {
        private const val AUTH_ENDPOINT = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"
        private const val PROJECT_ENDPOINT = "https://firestore.googleapis.com/v1/projects"
        private const val TRACK_IDS = "track_ids"
        private const val PLACES_TO_VISIT = "places_to_visit"
        private const val TRACK = "track"
    }
}
