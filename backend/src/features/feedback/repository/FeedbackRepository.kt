package nl.brul.backend.features.feedback.repository

import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackQuery
import java.util.UUID

interface FeedbackRepository {
    fun findById(id: UUID): Feedback?

    /** One page of the items that match [query], in the requested order. */
    fun search(query: FeedbackQuery): List<Feedback>

    /** How many items match the filters of [query] in total, ignoring its page. */
    fun count(query: FeedbackQuery): Long

    fun save(feedback: Feedback): Feedback

    /** Updates the message and the status; the owner and the creation time never change. */
    fun update(feedback: Feedback): Boolean

    /** Deletes the row for good; returns `false` when no item with [id] exists. */
    fun delete(id: UUID): Boolean
}
