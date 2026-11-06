package nl.example.backend.features.feedback.dto

import kotlinx.serialization.Serializable

/**
 * Body of both `POST /feedback` and `PATCH /feedback/{id}`. No owner and no status: the owner is the caller and
 * every new item starts as `OPEN`; the status has its own endpoint.
 */
@Serializable
data class FeedbackRequest(
    val message: String
)
