package nl.brul.backend.features.feedback

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.feedback.mapper.toResponse
import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackStatus
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackMapperTest {
    @Test
    fun `maps all fields`() {
        val feedback = Feedback(
            UUID.randomUUID(), UUID.randomUUID(), "The app is slow", FeedbackStatus.IN_PROGRESS, TEST_CREATED_AT
        )

        val response = feedback.toResponse()

        assertEquals(feedback.id.toString(), response.id)
        assertEquals(feedback.userId.toString(), response.userId)
        assertEquals("The app is slow", response.message)
        assertEquals("IN_PROGRESS", response.status)
        assertEquals("2026-01-01T12:00:00Z", response.createdAt)
    }
}
