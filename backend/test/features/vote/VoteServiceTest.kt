package nl.brul.backend.features.vote

import nl.brul.backend.features.feedback.FakeFeedbackRepository
import nl.brul.backend.features.feedback.dto.FeedbackRequest
import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.features.vote.dto.VoteRequest
import nl.brul.backend.features.vote.model.VoteType
import nl.brul.backend.features.vote.service.VoteService
import nl.brul.backend.shared.error.AppException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoteServiceTest {
    private val feedbackRepository = FakeFeedbackRepository()
    private val feedbackService = FeedbackService(feedbackRepository)
    private val voteRepository = FakeVoteRepository()
    private val voteService = VoteService(voteRepository, feedbackService)

    private val ownerId = UUID.randomUUID()
    private val voter1Id = UUID.randomUUID()
    private val voter2Id = UUID.randomUUID()

    private fun createFeedback(): UUID =
        UUID.fromString(feedbackService.create(ownerId, FeedbackRequest("Test feedback")).id)

    @Test
    fun `cast vote as LIKE creates a new vote`() {
        val feedbackId = createFeedback()

        val (response, isCreated) = voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.LIKE))

        assertTrue(isCreated)
        assertEquals(feedbackId.toString(), response.feedbackId)
        assertEquals(voter1Id.toString(), response.userId)
        assertEquals(VoteType.LIKE, response.type)
    }

    @Test
    fun `changing vote updates existing vote without creating duplicate`() {
        val feedbackId = createFeedback()

        val (firstResponse, isCreated) = voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.LIKE))
        assertTrue(isCreated)

        val (secondResponse, isUpdated) = voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.DISLIKE))
        assertFalse(isUpdated)
        assertEquals(firstResponse.id, secondResponse.id)
        assertEquals(VoteType.DISLIKE, secondResponse.type)
        assertEquals(1, voteRepository.findByFeedback(feedbackId).size)
    }

    @Test
    fun `owner cannot vote on their own feedback`() {
        val feedbackId = createFeedback()

        val error = assertFailsWith<AppException.Forbidden> {
            voteService.castVote(feedbackId, ownerId, VoteRequest(VoteType.LIKE))
        }
        assertEquals("FORBIDDEN", error.code)
    }

    @Test
    fun `voting on missing feedback throws 404`() {
        val missingFeedbackId = UUID.randomUUID()

        val error = assertFailsWith<AppException.NotFound> {
            voteService.castVote(missingFeedbackId, voter1Id, VoteRequest(VoteType.LIKE))
        }
        assertEquals("FEEDBACK_NOT_FOUND", error.code)
    }

    @Test
    fun `getUserVote returns the user vote`() {
        val feedbackId = createFeedback()
        voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.LIKE))

        val vote = voteService.getUserVote(feedbackId, voter1Id)

        assertEquals(voter1Id.toString(), vote.userId)
        assertEquals(VoteType.LIKE, vote.type)
    }

    @Test
    fun `getUserVote throws 404 when user has not voted`() {
        val feedbackId = createFeedback()

        val error = assertFailsWith<AppException.NotFound> {
            voteService.getUserVote(feedbackId, voter1Id)
        }
        assertEquals("VOTE_NOT_FOUND", error.code)
    }

    @Test
    fun `getSummary aggregates likes and dislikes`() {
        val feedbackId = createFeedback()
        voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.LIKE))
        voteService.castVote(feedbackId, voter2Id, VoteRequest(VoteType.DISLIKE))

        val summary = voteService.getSummary(feedbackId)

        assertEquals(1, summary.likes)
        assertEquals(1, summary.dislikes)
        assertEquals(0, summary.score)
        assertEquals(2, summary.totalVotes)
    }

    @Test
    fun `removeVote deletes the vote`() {
        val feedbackId = createFeedback()
        voteService.castVote(feedbackId, voter1Id, VoteRequest(VoteType.LIKE))

        voteService.removeVote(feedbackId, voter1Id)

        assertFailsWith<AppException.NotFound> {
            voteService.getUserVote(feedbackId, voter1Id)
        }
    }

    @Test
    fun `removeVote throws 404 when vote does not exist`() {
        val feedbackId = createFeedback()

        val error = assertFailsWith<AppException.NotFound> {
            voteService.removeVote(feedbackId, voter1Id)
        }
        assertEquals("VOTE_NOT_FOUND", error.code)
    }
}
