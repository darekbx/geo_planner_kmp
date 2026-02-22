package com.darekbx.geoplanner.kmp.cloud.dto

import com.darekbx.geoplanner.kmp.model.TrackPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentials(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
data class AuthResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

@Serializable
data class TrackIdsResponse(
    val documents: List<Document<TrackIdFields>> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class PlacesToVisitResponse(
    val documents: List<Document<PlacesToVisitFields>> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class TrackResponse(
    val documents: List<Document<TrackFields>> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class Document<T>(
    val name: String,
    val fields: T,
    val createTime: String,
    val updateTime: String
)

@Serializable
data class TrackIdFields(
    val id: IntegerValue
)

@Serializable
data class PlacesToVisitFields(
    val label: StringValue,
    val latitude: DoubleValue,
    val longitude: DoubleValue
)

/*
 "label": {
          "nullValue": null
        },
*/
@Serializable
data class TrackFields(
    @SerialName("local_id")
    val localId: IntegerValue,
    val label: StringValue,
    val distance: DoubleValue,
    @SerialName("start_timestamp")
    val startTimestamp: LongValue,
    @SerialName("end_timestamp")
    val endTimestamp: LongValue,
    val points: StringValue
)

@Serializable
data class LongValue(
    @SerialName("integerValue")
    val value: Long
)

@Serializable
data class IntegerValue(
    @SerialName("integerValue")
    val value: Int
)

@Serializable
data class DoubleValue(
    @SerialName("doubleValue")
    val value: Double
)


@Serializable
data class ArrayValue(
    @SerialName("arrayValue")
    val value: ArrayValues
)

@Serializable
data class ArrayValues(
    @SerialName("values")
    val values: List<LongValue>
)

@Serializable
data class StringValue(
    @SerialName("stringValue")
    val value: String = "",
    @SerialName("nullValue")
    val nullValue: String? = null
)

@Serializable
data class RunQueryRequest<T> (
    @SerialName("structuredQuery")
    val structuredQuery: StructuredQuery<T>
)

@Serializable
data class StructuredQuery<T> (
    val from: List<From>,
    val where: Where<T>? = null,
)

@Serializable
data class From(
    val collectionId: String
)

@Serializable
data class Where<T>(
    val fieldFilter: T
)

@Serializable
data class FieldFilter<T>(
    val field: FieldPath,
    val op: String,
    val value: T
)

@Serializable
data class FieldPath(
    val fieldPath: String
)

@Serializable
data class RunQueryResponse(
    val document: Document<TrackFields>? = null,
    val readTime: String? = null,
    val done: Boolean? = null
)