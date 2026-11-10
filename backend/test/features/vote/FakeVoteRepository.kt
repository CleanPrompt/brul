package nl.brul.backend.features.vote

import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteType
import nl.brul.backend.features.vote.repository.VoteRepository
import java.util.UUID

/** In-memory [VoteRepository] so service and route tests do not need a database. */
class FakeVoteRepository : VoteRepository {
    private val items = mutableMapOf<UUID, Vote>()

    override fun findByFeedbackAndUser(feedbackId: UUID, userId: UUID): Vote? =
        items.values.find { it.feedbackId == feedbackId && it.userId == userId }

    override fun findByFeedback(feedbackId: UUID): List<Vote> =
        items.values.filter { it.feedbackId == feedbackId }

    override fun save(vote: Vote): Vote {
        items[vote.id] = vote
        return vote
    }

    override fun update(feedbackId: UUID, userId: UUID, type: VoteType): Boolean {
        val existing = findByFeedbackAndUser(feedbackId, userId) ?: return false
        items[existing.id] = existing.copy(type = type)
        return true
    }

    override fun delete(feedbackId: UUID, userId: UUID): Boolean {
        val existing = findByFeedbackAndUser(feedbackId, userId) ?: return false
        return items.remove(existing.id) != null
    }
}
