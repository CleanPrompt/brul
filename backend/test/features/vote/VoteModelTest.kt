package nl.brul.backend.features.vote

import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteSummary
import nl.brul.backend.features.vote.model.VoteType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class VoteModelTest {

    @Test
    fun `LIKE is a supported vote type`() {
        assertEquals(VoteType.LIKE, VoteType.valueOf("LIKE"))
    }

    @Test
    fun `DISLIKE is a supported vote type`() {
        assertEquals(VoteType.DISLIKE, VoteType.valueOf("DISLIKE"))
    }

    @Test
    fun `valid vote can be created`() {
        val voteId = UUID.randomUUID()
        val feedbackId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val vote = Vote(
            id = voteId,
            feedbackId = feedbackId,
            userId = userId,
            type = VoteType.LIKE
        )

        assertEquals(voteId, vote.id)
        assertEquals(feedbackId, vote.feedbackId)
        assertEquals(userId, vote.userId)
        assertEquals(VoteType.LIKE, vote.type)
    }

    @Test
    fun `vote summary calculates score`() {
        val summary = VoteSummary(
            feedbackId = UUID.randomUUID(),
            likes = 8,
            dislikes = 3
        )

        assertEquals(5, summary.score)
    }

    @Test
    fun `vote summary calculates total votes`() {
        val summary = VoteSummary(
            feedbackId = UUID.randomUUID(),
            likes = 8,
            dislikes = 3
        )

        assertEquals(11, summary.totalVotes)
    }
}
