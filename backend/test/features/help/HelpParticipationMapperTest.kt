package nl.brul.backend.features.help

import nl.brul.backend.features.help.mapper.toResponse
import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.HelpParticipationSummary
import nl.brul.backend.features.help.model.ParticipationStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpParticipationMapperTest {

    private val id = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val helpRequestId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val timestamp = Instant.parse("2026-11-20T10:05:00Z")

    @Test
    fun `maps HelpParticipation with check-in timestamp to response`() {
        val participation = HelpParticipation(
            id = id,
            helpRequestId = helpRequestId,
            userId = userId,
            status = ParticipationStatus.ATTENDED,
            checkedInAt = timestamp
        )

        val response = participation.toResponse()

        assertEquals("11111111-1111-1111-1111-111111111111", response.id)
        assertEquals("22222222-2222-2222-2222-222222222222", response.helpRequestId)
        assertEquals("33333333-3333-3333-3333-333333333333", response.userId)
        assertEquals("ATTENDED", response.status)
        assertEquals("2026-11-20T10:05:00Z", response.checkedInAt)
    }

    @Test
    fun `maps HelpParticipation without check-in to response with null checkedInAt`() {
        val participation = HelpParticipation(
            id = id,
            helpRequestId = helpRequestId,
            userId = userId,
            status = ParticipationStatus.SIGNED_UP,
            checkedInAt = null
        )

        val response = participation.toResponse()

        assertEquals("SIGNED_UP", response.status)
        assertNull(response.checkedInAt)
    }

    @Test
    fun `maps HelpParticipationSummary to response`() {
        val summary = HelpParticipationSummary(
            helpRequestId = helpRequestId,
            volunteersNeeded = 3,
            participantCount = 3,
            availablePlaces = 0,
            isFull = true
        )

        val response = summary.toResponse()

        assertEquals("22222222-2222-2222-2222-222222222222", response.helpRequestId)
        assertEquals(3, response.volunteersNeeded)
        assertEquals(3, response.participantCount)
        assertEquals(0, response.availablePlaces)
        assertTrue(response.isFull)
    }
}
