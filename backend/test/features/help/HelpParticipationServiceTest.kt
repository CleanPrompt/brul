package nl.brul.backend.features.help

import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.help.service.ActiveCheckInCode
import nl.brul.backend.features.help.service.HelpParticipationService
import nl.brul.backend.features.help.service.HelpRequestInfo
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HelpParticipationServiceTest {

    private val repo = FakeHelpParticipationRepository()
    private val provider = FakeHelpRequestProvider()
    private var currentTime = Instant.parse("2026-11-20T10:00:00Z")

    private val service = HelpParticipationService(
        participationRepository = repo,
        helpRequestProvider = provider,
        currentTime = { currentTime }
    )

    private val ownerId = UUID.randomUUID()
    private val helpRequestId = UUID.randomUUID()
    private val volunteerId = UUID.randomUUID()
    private val location = Location(51.582, 4.771)

    private val defaultRequest = HelpRequestInfo(
        id = helpRequestId,
        ownerId = ownerId,
        title = "Tuinieren in Breda",
        description = "Samen de voortuin opknappen",
        category = "CHORES",
        location = location,
        volunteersNeeded = 2,
        isOpen = true,
        startTime = Instant.parse("2026-11-20T10:00:00Z"),
        endTime = Instant.parse("2026-11-20T12:00:00Z")
    )

    @BeforeTest
    fun setUp() {
        repo.clear()
        provider.clear()
        provider.requests[helpRequestId] = defaultRequest
        currentTime = Instant.parse("2026-11-20T10:00:00Z")
    }

    @Test
    fun `join adds a volunteer and returns participation`() {
        val participation = service.join(helpRequestId, volunteerId)

        assertEquals(helpRequestId, participation.helpRequestId)
        assertEquals(volunteerId, participation.userId)
        assertEquals(ParticipationStatus.SIGNED_UP, participation.status)
        assertEquals(1, repo.countByHelpRequest(helpRequestId))
    }

    @Test
    fun `join throws Forbidden when owner tries to join own help request`() {
        assertFailsWith<AppException.Forbidden> {
            service.join(helpRequestId, ownerId)
        }
    }

    @Test
    fun `join throws Conflict when help request is closed`() {
        provider.requests[helpRequestId] = defaultRequest.copy(isOpen = false)

        assertFailsWith<AppException.Conflict> {
            service.join(helpRequestId, volunteerId)
        }
    }

    @Test
    fun `join throws Conflict when user already participates`() {
        service.join(helpRequestId, volunteerId)

        assertFailsWith<AppException.Conflict> {
            service.join(helpRequestId, volunteerId)
        }
    }

    @Test
    fun `join throws Conflict when help request is full`() {
        val requestWithCapacity1 = defaultRequest.copy(volunteersNeeded = 1)
        provider.requests[helpRequestId] = requestWithCapacity1

        service.join(helpRequestId, volunteerId)

        val secondVolunteer = UUID.randomUUID()
        assertFailsWith<AppException.Conflict> {
            service.join(helpRequestId, secondVolunteer)
        }
    }

    @Test
    fun `leave removes a participation`() {
        service.join(helpRequestId, volunteerId)
        assertEquals(1, repo.countByHelpRequest(helpRequestId))

        val removed = service.leave(helpRequestId, volunteerId)

        assertTrue(removed)
        assertEquals(0, repo.countByHelpRequest(helpRequestId))
    }

    @Test
    fun `leave throws NotFound for missing participation`() {
        assertFailsWith<AppException.NotFound> {
            service.leave(helpRequestId, volunteerId)
        }
    }

    @Test
    fun `getSummary calculates available places and full state accurately`() {
        val summaryBefore = service.getSummary(helpRequestId)
        assertEquals(2, summaryBefore.volunteersNeeded)
        assertEquals(0, summaryBefore.participantCount)
        assertEquals(2, summaryBefore.availablePlaces)
        assertFalse(summaryBefore.isFull)

        service.join(helpRequestId, volunteerId)
        service.join(helpRequestId, UUID.randomUUID())

        val summaryAfter = service.getSummary(helpRequestId)
        assertEquals(2, summaryAfter.participantCount)
        assertEquals(0, summaryAfter.availablePlaces)
        assertTrue(summaryAfter.isFull)
    }

    @Test
    fun `generateCheckInCode succeeds for owner`() {
        val response = service.generateCheckInCode(helpRequestId, ownerId)

        assertEquals(helpRequestId.toString(), response.helpRequestId)
        assertEquals(6, response.code.length)
        assertNotNull(provider.getCheckInCode(helpRequestId))
    }

    @Test
    fun `generateCheckInCode throws Forbidden for non-owner`() {
        assertFailsWith<AppException.Forbidden> {
            service.generateCheckInCode(helpRequestId, volunteerId)
        }
    }

    @Test
    fun `generateCheckInCode throws Conflict for closed request`() {
        provider.requests[helpRequestId] = defaultRequest.copy(isOpen = false)

        assertFailsWith<AppException.Conflict> {
            service.generateCheckInCode(helpRequestId, ownerId)
        }
    }

    @Test
    fun `checkIn succeeds when code, time and location are valid`() {
        service.join(helpRequestId, volunteerId)
        provider.setCheckInCode(helpRequestId, "123456", defaultRequest.endTime)
        currentTime = Instant.parse("2026-11-20T10:05:00Z")

        val response = service.checkIn(
            helpRequestId = helpRequestId,
            userId = volunteerId,
            code = "123456",
            volunteerLocation = Location(51.5821, 4.7711)
        )

        assertTrue(response.success)
        val loaded = repo.findByHelpRequestAndUser(helpRequestId, volunteerId)
        assertNotNull(loaded)
        assertEquals(ParticipationStatus.ATTENDED, loaded.status)
        assertEquals(currentTime, loaded.checkedInAt)
    }

    @Test
    fun `checkIn throws BadRequest for invalid code`() {
        service.join(helpRequestId, volunteerId)
        provider.setCheckInCode(helpRequestId, "123456", defaultRequest.endTime)

        assertFailsWith<AppException.BadRequest> {
            service.checkIn(helpRequestId, volunteerId, "000000", location)
        }
    }

    @Test
    fun `checkIn throws BadRequest when too early`() {
        service.join(helpRequestId, volunteerId)
        provider.setCheckInCode(helpRequestId, "123456", defaultRequest.endTime)
        // 20 minutes before 10:00
        currentTime = Instant.parse("2026-11-20T09:40:00Z")

        assertFailsWith<AppException.BadRequest> {
            service.checkIn(helpRequestId, volunteerId, "123456", location)
        }
    }

    @Test
    fun `checkIn throws BadRequest when too far away`() {
        service.join(helpRequestId, volunteerId)
        provider.setCheckInCode(helpRequestId, "123456", defaultRequest.endTime)
        currentTime = Instant.parse("2026-11-20T10:05:00Z")
        val farLocation = Location(51.595, 4.771) // ~1.5 km away

        assertFailsWith<AppException.BadRequest> {
            service.checkIn(helpRequestId, volunteerId, "123456", farLocation)
        }
    }

    @Test
    fun `checkIn throws NotFound when user is not registered`() {
        provider.setCheckInCode(helpRequestId, "123456", defaultRequest.endTime)

        assertFailsWith<AppException.NotFound> {
            service.checkIn(helpRequestId, volunteerId, "123456", location)
        }
    }

    @Test
    fun `getCalendarFeed returns valid RFC 5545 ics text`() {
        val ics = service.getCalendarFeed(helpRequestId)

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.contains("SUMMARY:Brul Hulp: Tuinieren in Breda\r\n"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }
}
