package nl.brul.backend.features.help.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckInRequest(
    val code: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class CheckInResponse(
    val success: Boolean,
    val message: String,
    /** ISO 8601 in UTC when checked in, e.g. `2026-11-20T10:05:00Z`. */
    val checkedInAt: String? = null
)

@Serializable
data class CheckInCodeResponse(
    val helpRequestId: String,
    val code: String,
    /** ISO 8601 in UTC when the code expires. */
    val expiresAt: String
)
