package nl.brul.backend.features.vote

import nl.brul.backend.features.vote.mapper.toResponse
import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteSummary
import nl.brul.backend.features.vote.model.VoteType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class VoteMapperTest {

    @Test
    fun `vote maps to response`() {
        val voteId = UUID.randomUUID()
        val feedbackId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val vote = Vote(
            id = voteId,
            feedbackId = feedbackId,
            userId = userId,
            type = VoteType.LIKE
        )

        val response = vote.toResponse()

        assertEquals(voteId.toString(), response.id)
        assertEquals(feedbackId.toString(), response.feedbackId)
        assertEquals(userId.toString(), response.userId)
        assertEquals(VoteType.LIKE, response.type)
    }

    @Test
    fun `vote summary maps to response`() {
        val feedbackId = UUID.randomUUID()
        val summary = VoteSummary(
            feedbackId = feedbackId,
            likes = 7,
            dislikes = 2
        )

        val response = summary.toResponse()

        assertEquals(feedbackId.toString(), response.feedbackId)
        assertEquals(7, response.likes)
        assertEquals(2, response.dislikes)
        assertEquals(5, response.score)
        assertEquals(9, response.totalVotes)
    }
}
