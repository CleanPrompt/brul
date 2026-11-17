package nl.brul.backend.features.feedback.repository

import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackQuery
import nl.brul.backend.features.feedback.model.FeedbackStatus
import java.util.UUID

interface FeedbackRepository {
    fun findById(id: UUID): Feedback?

    /** One page of the items that match [query], in the requested order. */
    fun search(query: FeedbackQuery): List<Feedback>

    /** How many items match the filters of [query] in total, ignoring its page. */
    fun count(query: FeedbackQuery): Long

    fun save(feedback: Feedback): Feedback

    // Each change writes only its own column, so an admin changing the status and the owner editing the message at
    // the same time cannot undo each other. Both return `false` when no item with [id] exists. The owner and the
    // creation time never change.

    fun updateMessage(id: UUID, message: String): Boolean

    fun updateStatus(id: UUID, status: FeedbackStatus): Boolean

    /** Deletes the row for good; returns `false` when no item with [id] exists. */
    fun delete(id: UUID): Boolean
}
