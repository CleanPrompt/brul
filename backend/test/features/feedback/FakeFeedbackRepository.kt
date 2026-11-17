package nl.brul.backend.features.feedback

import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackQuery
import nl.brul.backend.features.feedback.model.FeedbackSortField
import nl.brul.backend.features.feedback.model.FeedbackStatus
import nl.brul.backend.features.feedback.repository.FeedbackRepository
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/** In-memory [FeedbackRepository] so service and route tests do not need a database. */
class FakeFeedbackRepository : FeedbackRepository {
    private val items = mutableMapOf<UUID, Feedback>()

    override fun findById(id: UUID): Feedback? = items[id]

    /** Same filters, order and page as the SQL query in `ExposedFeedbackRepository`. */
    override fun search(query: FeedbackQuery): List<Feedback> {
        val sorted = when (query.sortField) {
            FeedbackSortField.CREATED_AT -> matching(query).sortedBy { it.createdAt }
            FeedbackSortField.STATUS -> matching(query).sortedBy { it.status.name }
        }
        val ordered = if (query.sortDirection == SortDirection.DESC) sorted.reversed() else sorted
        return ordered.drop(query.page.offset).take(query.page.limit)
    }

    override fun count(query: FeedbackQuery): Long = matching(query).size.toLong()

    override fun save(feedback: Feedback): Feedback = feedback.also { items[it.id] = it }

    // Like the real repository: each function changes only its own field.

    override fun updateMessage(id: UUID, message: String): Boolean {
        val feedback = items[id] ?: return false
        items[id] = feedback.copy(message = message)
        return true
    }

    override fun updateStatus(id: UUID, status: FeedbackStatus): Boolean {
        val feedback = items[id] ?: return false
        items[id] = feedback.copy(status = status)
        return true
    }

    override fun delete(id: UUID): Boolean = items.remove(id) != null

    private fun matching(query: FeedbackQuery): List<Feedback> =
        items.values.filter { item ->
            (query.ownerId == null || item.ownerId == query.ownerId) &&
                (query.status == null || item.status == query.status) &&
                (query.text == null || item.message.contains(query.text, ignoreCase = true))
        }
}
