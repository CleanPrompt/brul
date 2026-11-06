package nl.brul.backend.features.feedback

import nl.brul.backend.features.feedback.dto.FeedbackRequest
import nl.brul.backend.features.feedback.dto.UpdateFeedbackStatusRequest
import nl.brul.backend.features.feedback.model.FeedbackQuery
import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.paging.Page
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedbackServiceTest {
    private val repository = FakeFeedbackRepository()
    private val service = FeedbackService(repository)
    private val ownerId = UUID.randomUUID()

    private fun create(message: String = "The app is slow"): UUID =
        UUID.fromString(service.create(ownerId, FeedbackRequest(message)).id)

    @Test
    fun `create sets the owner, trims the message and starts as OPEN`() {
        val response = service.create(ownerId, FeedbackRequest("  The app is slow  "))

        assertEquals(ownerId.toString(), response.userId)
        assertEquals("The app is slow", response.message)
        assertEquals("OPEN", response.status)
        assertTrue(response.createdAt.endsWith("Z"), "createdAt must be ISO 8601 UTC")
    }

    @Test
    fun `rejects an empty or too long message`() {
        assertFailsWith<AppException.BadRequest> { service.create(ownerId, FeedbackRequest("   ")) }
        assertFailsWith<AppException.BadRequest> { service.create(ownerId, FeedbackRequest("x".repeat(2001))) }
    }

    @Test
    fun `search returns one page and the total number of matches`() {
        repeat(3) { create() }
        service.create(UUID.randomUUID(), FeedbackRequest("Someone else"))

        val page = service.search(FeedbackQuery(userId = ownerId, page = Page(limit = 2, offset = 0)))

        assertEquals(2, page.items.size)
        assertEquals(3, page.total)
        assertEquals(2, page.limit)
        assertEquals(0, page.offset)
    }

    @Test
    fun `getOwnerId returns the owner`() {
        assertEquals(ownerId, service.getOwnerId(create()))
    }

    @Test
    fun `updateMessage changes the message and keeps the status`() {
        val id = create()

        val response = service.updateMessage(id, FeedbackRequest("The app is fast now"))

        assertEquals("The app is fast now", response.message)
        assertEquals("OPEN", repository.findById(id)?.status?.name)
    }

    @Test
    fun `updateStatus changes the status`() {
        val id = create()

        val response = service.updateStatus(id, UpdateFeedbackStatusRequest("IN_PROGRESS"))

        assertEquals("IN_PROGRESS", response.status)
        assertEquals("IN_PROGRESS", repository.findById(id)?.status?.name)
    }

    @Test
    fun `rejects an unknown status`() {
        val id = create()

        assertFailsWith<AppException.BadRequest> { service.updateStatus(id, UpdateFeedbackStatusRequest("DONE")) }
    }

    @Test
    fun `delete removes the item`() {
        val id = create()

        service.delete(id)

        assertNull(repository.findById(id))
    }

    @Test
    fun `missing feedback gives 404`() {
        val missing = UUID.randomUUID()

        val error = assertFailsWith<AppException.NotFound> { service.getById(missing) }
        assertEquals("FEEDBACK_NOT_FOUND", error.code)
        assertFailsWith<AppException.NotFound> { service.getOwnerId(missing) }
        assertFailsWith<AppException.NotFound> { service.delete(missing) }
    }
}
