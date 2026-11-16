package nl.brul.backend.features.feedback.mapper

import nl.brul.backend.features.feedback.database.FeedbackTable
import nl.brul.backend.features.feedback.model.Feedback
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFeedback(): Feedback =
    Feedback(
        id = this[FeedbackTable.id],
        ownerId = this[FeedbackTable.ownerId],
        message = this[FeedbackTable.message],
        status = this[FeedbackTable.status],
        createdAt = this[FeedbackTable.createdAt]
    )
