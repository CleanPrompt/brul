package nl.example.backend.features.feedback.dto

import kotlinx.serialization.Serializable

/**
 * [status] is a plain string so `FeedbackService` can reject an unknown value with a clear 400 message,
 * e.g. `"IN_PROGRESS"`.
 */
@Serializable
data class UpdateFeedbackStatusRequest(
    val status: String
)
