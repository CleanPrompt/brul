package nl.brul.backend.features.feedback.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackResponse(
    val id: String,
    /** The owner's user id. */
    val userId: String,
    val message: String,
    /** `OPEN`, `IN_PROGRESS` or `CLOSED`. */
    val status: String,
    /** ISO 8601 in UTC, e.g. `2026-11-05T09:30:00Z`. */
    val createdAt: String
)
