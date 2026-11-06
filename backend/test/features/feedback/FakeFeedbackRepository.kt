package nl.example.backend.features.feedback

import nl.example.backend.features.feedback.model.Feedback
import nl.example.backend.features.feedback.model.FeedbackQuery
import nl.example.backend.features.feedback.model.FeedbackSortField
import nl.example.backend.features.feedback.repository.FeedbackRepository
import nl.example.backend.shared.paging.SortDirection
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

    override fun update(feedback: Feedback): Boolean {
        val existing = items[feedback.id] ?: return false
        // Like the real repository: only the message and the status change.
        items[feedback.id] = existing.copy(message = feedback.message, status = feedback.status)
        return true
    }

    override fun delete(id: UUID): Boolean = items.remove(id) != null

    private fun matching(query: FeedbackQuery): List<Feedback> =
        items.values.filter { item ->
            (query.userId == null || item.userId == query.userId) &&
                (query.status == null || item.status == query.status) &&
                (query.text == null || item.message.contains(query.text, ignoreCase = true))
        }
}
