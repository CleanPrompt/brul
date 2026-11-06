package nl.example.backend.features.feedback

import nl.example.backend.TEST_CREATED_AT
import nl.example.backend.features.feedback.database.FeedbackTable
import nl.example.backend.features.feedback.model.Feedback
import nl.example.backend.features.feedback.model.FeedbackQuery
import nl.example.backend.features.feedback.model.FeedbackStatus
import nl.example.backend.features.feedback.repository.ExposedFeedbackRepository
import nl.example.backend.features.user.database.UsersTable
import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.repository.ExposedUserRepository
import nl.example.backend.shared.paging.Page
import nl.example.backend.shared.paging.SortDirection
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedbackRepositoryTest {
    private val repository = ExposedFeedbackRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:feedback_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, FeedbackTable) }
    }

    /** The foreign key needs an existing user. */
    private fun newOwner(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Owner", "hash", true, TEST_CREATED_AT)
        ).id

    private fun newFeedback(userId: UUID, createdAt: Instant = TEST_CREATED_AT) =
        Feedback(UUID.randomUUID(), userId, "The app is slow", FeedbackStatus.OPEN, createdAt)

    @Test
    fun `saves and retrieves feedback`() {
        val feedback = repository.save(newFeedback(newOwner()))

        assertEquals(feedback, repository.findById(feedback.id))
    }

    // The in-memory database is shared by the tests in this class, so every search test filters on a new owner.

    @Test
    fun `search filters on the owner and returns the newest first by default`() {
        val owner = newOwner()
        val older = repository.save(newFeedback(owner, TEST_CREATED_AT))
        val newer = repository.save(newFeedback(owner, TEST_CREATED_AT.plusSeconds(60)))
        repository.save(newFeedback(newOwner()))

        assertEquals(listOf(newer, older), repository.search(FeedbackQuery(userId = owner)))
        assertEquals(2, repository.count(FeedbackQuery(userId = owner)))
    }

    @Test
    fun `search filters on status and on text, ignoring case`() {
        val owner = newOwner()
        val slow = repository.save(newFeedback(owner).copy(message = "The app is SLOW"))
        repository.save(newFeedback(owner).copy(message = "Crash on start", status = FeedbackStatus.CLOSED))

        assertEquals(listOf(slow), repository.search(FeedbackQuery(userId = owner, text = "slow")))
        assertEquals(listOf(slow), repository.search(FeedbackQuery(userId = owner, status = FeedbackStatus.OPEN)))
        assertEquals(0, repository.count(FeedbackQuery(userId = owner, text = "slow", status = FeedbackStatus.CLOSED)))
    }

    @Test
    fun `percent sign in the search text is matched literally`() {
        val owner = newOwner()
        val discount = repository.save(newFeedback(owner).copy(message = "Give 50% discount"))
        repository.save(newFeedback(owner).copy(message = "50 people complained"))

        assertEquals(listOf(discount), repository.search(FeedbackQuery(userId = owner, text = "50%")))
    }

    @Test
    fun `search sorts ascending and returns the requested page`() {
        val owner = newOwner()
        val first = repository.save(newFeedback(owner, TEST_CREATED_AT))
        val second = repository.save(newFeedback(owner, TEST_CREATED_AT.plusSeconds(60)))
        val third = repository.save(newFeedback(owner, TEST_CREATED_AT.plusSeconds(120)))
        val ascending = FeedbackQuery(userId = owner, sortDirection = SortDirection.ASC)
        val firstPage = ascending.copy(page = Page(limit = 2, offset = 0))
        val secondPage = ascending.copy(page = Page(limit = 2, offset = 2))

        assertEquals(listOf(first, second), repository.search(firstPage))
        assertEquals(listOf(third), repository.search(secondPage))
        assertEquals(3, repository.count(secondPage))
    }

    @Test
    fun `updates the message and the status but not the owner`() {
        val feedback = repository.save(newFeedback(newOwner()))

        val updated = repository.update(
            feedback.copy(message = "Fixed", status = FeedbackStatus.CLOSED, userId = UUID.randomUUID())
        )

        assertTrue(updated)
        assertEquals(feedback.copy(message = "Fixed", status = FeedbackStatus.CLOSED), repository.findById(feedback.id))
    }

    @Test
    fun `delete removes the row and reports a missing row`() {
        val feedback = repository.save(newFeedback(newOwner()))

        assertTrue(repository.delete(feedback.id))
        assertNull(repository.findById(feedback.id))
        assertFalse(repository.delete(feedback.id))
    }

    @Test
    fun `rejects feedback for a user that does not exist`() {
        assertFailsWith<ExposedSQLException> { repository.save(newFeedback(UUID.randomUUID())) }
    }
}
