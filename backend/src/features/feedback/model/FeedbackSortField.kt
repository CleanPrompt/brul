package nl.example.backend.features.feedback.model

/** Fields a feedback list can be sorted on; [key] is the value of the `sort` query parameter. */
enum class FeedbackSortField(val key: String) {
    CREATED_AT("createdAt"),
    /** Alphabetical by status name: `CLOSED`, `IN_PROGRESS`, `OPEN`. */
    STATUS("status")
}
