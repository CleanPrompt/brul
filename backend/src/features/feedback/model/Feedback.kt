package nl.example.backend.features.feedback.model

import java.time.Instant
import java.util.UUID

data class Feedback(
    val id: UUID,
    /** The owner: the user who created the item. Never changes. */
    val userId: UUID,
    val message: String,
    val status: FeedbackStatus,
    /** Set by the server when the item is created (UTC); never changed afterwards. */
    val createdAt: Instant
)
