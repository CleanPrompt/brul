package nl.brul.backend.features.vote.service

import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.features.vote.dto.VoteRequest
import nl.brul.backend.features.vote.dto.VoteResponse
import nl.brul.backend.features.vote.dto.VoteSummaryResponse
import nl.brul.backend.features.vote.mapper.toResponse
import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteSummary
import nl.brul.backend.features.vote.model.VoteType
import nl.brul.backend.features.vote.repository.VoteRepository
import nl.brul.backend.shared.error.AppException
import java.util.UUID

class VoteService(
    private val voteRepository: VoteRepository,
    private val feedbackService: FeedbackService
) {
    /**
     * Casts or updates a vote.
     * Returns a pair with the response and `true` if a new vote was created, or `false` if an existing vote was updated.
     */
    fun castVote(feedbackId: UUID, userId: UUID, request: VoteRequest): Pair<VoteResponse, Boolean> {
        val ownerId = feedbackService.getOwnerId(feedbackId)
        if (ownerId == userId) {
            throw AppException.Forbidden("Users cannot vote on their own feedback")
        }

        val existing = voteRepository.findByFeedbackAndUser(feedbackId, userId)
        return if (existing == null) {
            val vote = Vote(
                id = UUID.randomUUID(),
                feedbackId = feedbackId,
                userId = userId,
                type = request.type
            )
            voteRepository.save(vote).toResponse() to true
        } else {
            voteRepository.update(feedbackId, userId, request.type)
            existing.copy(type = request.type).toResponse() to false
        }
    }

    /** Returns the authenticated user's vote for the given feedback item, or throws 404. */
    fun getUserVote(feedbackId: UUID, userId: UUID): VoteResponse {
        feedbackService.getOwnerId(feedbackId)
        val vote = voteRepository.findByFeedbackAndUser(feedbackId, userId)
            ?: throw AppException.NotFound("Vote not found", "VOTE_NOT_FOUND")
        return vote.toResponse()
    }

    /** Aggregates likes and dislikes for the given feedback item. */
    fun getSummary(feedbackId: UUID): VoteSummaryResponse {
        feedbackService.getOwnerId(feedbackId)
        val votes = voteRepository.findByFeedback(feedbackId)
        return VoteSummary(
            feedbackId = feedbackId,
            likes = votes.count { it.type == VoteType.LIKE },
            dislikes = votes.count { it.type == VoteType.DISLIKE }
        ).toResponse()
    }

    /** Deletes the authenticated user's vote for the given feedback item, or throws 404. */
    fun removeVote(feedbackId: UUID, userId: UUID) {
        feedbackService.getOwnerId(feedbackId)
        if (!voteRepository.delete(feedbackId, userId)) {
            throw AppException.NotFound("Vote not found", "VOTE_NOT_FOUND")
        }
    }
}
