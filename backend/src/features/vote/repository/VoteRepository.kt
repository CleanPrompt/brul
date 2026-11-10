package nl.brul.backend.features.vote.repository

import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteType
import java.util.UUID

interface VoteRepository {
    fun findByFeedbackAndUser(feedbackId: UUID, userId: UUID): Vote?
    fun findByFeedback(feedbackId: UUID): List<Vote>
    fun save(vote: Vote): Vote
    fun update(feedbackId: UUID, userId: UUID, type: VoteType): Boolean
    fun delete(feedbackId: UUID, userId: UUID): Boolean
}
