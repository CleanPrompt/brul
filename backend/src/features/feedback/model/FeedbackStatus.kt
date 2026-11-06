package nl.example.backend.features.feedback.model

/** Where a feedback item is in its handling. New items start as [OPEN]. */
enum class FeedbackStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}
