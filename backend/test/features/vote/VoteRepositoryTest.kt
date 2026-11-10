package nl.brul.backend.features.vote

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.feedback.database.FeedbackTable
import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackStatus
import nl.brul.backend.features.feedback.repository.ExposedFeedbackRepository
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.features.vote.database.VotesTable
import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteType
import nl.brul.backend.features.vote.repository.ExposedVoteRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoteRepositoryTest {
    private val voteRepository = ExposedVoteRepository()
    private val feedbackRepository = ExposedFeedbackRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:vote_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, FeedbackTable, VotesTable) }
    }

    private fun newUser(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Test User", "hash", true, TEST_CREATED_AT)
        ).id

    private fun newFeedback(ownerId: UUID): UUID {
        val feedback = Feedback(UUID.randomUUID(), ownerId, "Feedback message", FeedbackStatus.OPEN, TEST_CREATED_AT)
        return feedbackRepository.save(feedback).id
    }

    @Test
    fun `save and find vote by feedback and user`() {
        val feedbackId = newFeedback(newUser())
        val voterId = newUser()

        val vote = voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voterId, VoteType.LIKE))

        val found = voteRepository.findByFeedbackAndUser(feedbackId, voterId)
        assertNotNull(found)
        assertEquals(vote.id, found.id)
        assertEquals(VoteType.LIKE, found.type)
    }

    @Test
    fun `unknown vote returns null`() {
        assertNull(voteRepository.findByFeedbackAndUser(UUID.randomUUID(), UUID.randomUUID()))
    }

    @Test
    fun `update changes vote type`() {
        val feedbackId = newFeedback(newUser())
        val voterId = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voterId, VoteType.LIKE))
        val updated = voteRepository.update(feedbackId, voterId, VoteType.DISLIKE)

        assertTrue(updated)
        val found = voteRepository.findByFeedbackAndUser(feedbackId, voterId)
        assertNotNull(found)
        assertEquals(VoteType.DISLIKE, found.type)
    }

    @Test
    fun `multiple users can vote on same feedback`() {
        val feedbackId = newFeedback(newUser())
        val voter1 = newUser()
        val voter2 = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter1, VoteType.LIKE))
        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter2, VoteType.DISLIKE))

        val votes = voteRepository.findByFeedback(feedbackId)
        assertEquals(2, votes.size)
    }

    @Test
    fun `same user can vote on different feedback`() {
        val owner = newUser()
        val feedback1 = newFeedback(owner)
        val feedback2 = newFeedback(owner)
        val voter = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedback1, voter, VoteType.LIKE))
        voteRepository.save(Vote(UUID.randomUUID(), feedback2, voter, VoteType.DISLIKE))

        assertEquals(1, voteRepository.findByFeedback(feedback1).size)
        assertEquals(1, voteRepository.findByFeedback(feedback2).size)
    }

    @Test
    fun `delete existing vote returns true`() {
        val feedbackId = newFeedback(newUser())
        val voter = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter, VoteType.LIKE))

        assertTrue(voteRepository.delete(feedbackId, voter))
        assertNull(voteRepository.findByFeedbackAndUser(feedbackId, voter))
        assertFalse(voteRepository.delete(feedbackId, voter))
    }

    @Test
    fun `deleting feedback cascades and removes associated votes`() {
        val feedbackId = newFeedback(newUser())
        val voter1 = newUser()
        val voter2 = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter1, VoteType.LIKE))
        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter2, VoteType.DISLIKE))

        assertEquals(2, voteRepository.findByFeedback(feedbackId).size)

        feedbackRepository.delete(feedbackId)

        assertEquals(0, voteRepository.findByFeedback(feedbackId).size)
    }

    @Test
    fun `deleting one feedback preserves votes on other feedback`() {
        val owner = newUser()
        val feedback1 = newFeedback(owner)
        val feedback2 = newFeedback(owner)
        val voter = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedback1, voter, VoteType.LIKE))
        voteRepository.save(Vote(UUID.randomUUID(), feedback2, voter, VoteType.LIKE))

        feedbackRepository.delete(feedback1)

        assertEquals(0, voteRepository.findByFeedback(feedback1).size)
        assertEquals(1, voteRepository.findByFeedback(feedback2).size)
    }

    @Test
    fun `duplicate vote for same feedback and user violates unique constraint`() {
        val feedbackId = newFeedback(newUser())
        val voter = newUser()

        voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter, VoteType.LIKE))

        assertFailsWith<ExposedSQLException> {
            voteRepository.save(Vote(UUID.randomUUID(), feedbackId, voter, VoteType.DISLIKE))
        }
    }

    @Test
    fun `rejects vote for non-existent feedback or user`() {
        val voter = newUser()
        assertFailsWith<ExposedSQLException> {
            voteRepository.save(Vote(UUID.randomUUID(), UUID.randomUUID(), voter, VoteType.LIKE))
        }
    }
}
