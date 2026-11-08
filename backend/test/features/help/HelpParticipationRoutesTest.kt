package nl.brul.backend.features.help

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.errorCode
import nl.brul.backend.features.help.dto.CheckInCodeResponse
import nl.brul.backend.features.help.dto.CheckInResponse
import nl.brul.backend.features.help.dto.HelpParticipationResponse
import nl.brul.backend.features.help.dto.HelpParticipationSummaryResponse
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.service.HelpRequestInfo
import nl.brul.backend.register
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HelpParticipationRoutesTest {

    private fun sampleHelpRequest(
        ownerId: UUID,
        volunteersNeeded: Int = 3,
        isOpen: Boolean = true,
        startTime: Instant = Instant.now().minus(30, ChronoUnit.MINUTES),
        endTime: Instant = Instant.now().plus(2, ChronoUnit.HOURS),
        location: Location = Location(51.9244, 4.4777)
    ): HelpRequestInfo = HelpRequestInfo(
        id = UUID.randomUUID(),
        ownerId = ownerId,
        title = "Sneeuw ruimen stoep",
        description = "Hulp gevraagd bij het vrijmaken van de stoep voor de buurt.",
        category = "Wijkonderhoud",
        location = location,
        volunteersNeeded = volunteersNeeded,
        isOpen = isOpen,
        startTime = startTime,
        endTime = endTime
    )

    @Test
    fun `public calendar feed returns 200 with text calendar contentType and valid ICS content`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val ownerId = UUID.randomUUID()
        val request = sampleHelpRequest(ownerId)
        services.helpRequestProvider.requests[request.id] = request

        val response = client.get("/api/v1/help-requests/${request.id}/calendar.ics")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType]?.startsWith("text/calendar") == true)
        assertTrue(response.headers[HttpHeaders.ContentDisposition]?.contains("help-request-${request.id}.ics") == true)

        val body = response.bodyAsText()
        assertTrue(body.contains("BEGIN:VCALENDAR"))
        assertTrue(body.contains("SUMMARY:Brul Hulp: Sneeuw ruimen stoep"))
        assertTrue(body.contains("END:VCALENDAR"))
    }

    @Test
    fun `public calendar feed for unknown id returns 404`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val response = client.get("/api/v1/help-requests/${UUID.randomUUID()}/calendar.ics")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("HELP_REQUEST_NOT_FOUND", response.errorCode())
    }

    @Test
    fun `public participant summary returns 200 with correct capacity counts`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val ownerId = UUID.randomUUID()
        val request = sampleHelpRequest(ownerId, volunteersNeeded = 5)
        services.helpRequestProvider.requests[request.id] = request

        val response = client.get("/api/v1/help-requests/${request.id}/participants/summary")

        assertEquals(HttpStatusCode.OK, response.status)
        val summary = Json.decodeFromString<HelpParticipationSummaryResponse>(response.bodyAsText())
        assertEquals(request.id.toString(), summary.helpRequestId)
        assertEquals(5, summary.volunteersNeeded)
        assertEquals(0, summary.participantCount)
        assertEquals(5, summary.availablePlaces)
        assertEquals(false, summary.isFull)
    }

    @Test
    fun `protected routes return 401 Unauthorized without bearer token`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val randomId = UUID.randomUUID()

        val getParticipants = client.get("/api/v1/help-requests/$randomId/participants")
        val postJoin = client.post("/api/v1/help-requests/$randomId/participants")
        val deleteMe = client.delete("/api/v1/help-requests/$randomId/participants/me")
        val postCode = client.post("/api/v1/help-requests/$randomId/check-in-code")
        val postCheckIn = client.post("/api/v1/help-requests/$randomId/check-in")

        assertEquals(HttpStatusCode.Unauthorized, getParticipants.status)
        assertEquals(HttpStatusCode.Unauthorized, postJoin.status)
        assertEquals(HttpStatusCode.Unauthorized, deleteMe.status)
        assertEquals(HttpStatusCode.Unauthorized, postCode.status)
        assertEquals(HttpStatusCode.Unauthorized, postCheckIn.status)
    }

    @Test
    fun `volunteer can join, list participants, and cannot join twice`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val owner = register("owner@example.com")
        val volunteer = register("volunteer@example.com")
        val request = sampleHelpRequest(UUID.fromString(owner.user.id))
        services.helpRequestProvider.requests[request.id] = request

        // 1. Volunteer joins
        val joinResponse = client.post("/api/v1/help-requests/${request.id}/participants") {
            bearerAuth(volunteer.accessToken)
        }
        assertEquals(HttpStatusCode.Created, joinResponse.status)
        val joined = Json.decodeFromString<HelpParticipationResponse>(joinResponse.bodyAsText())
        assertEquals(volunteer.user.id, joined.userId)
        assertEquals(request.id.toString(), joined.helpRequestId)
        assertEquals("SIGNED_UP", joined.status)

        // 2. Joining again returns 409 Conflict
        val duplicateResponse = client.post("/api/v1/help-requests/${request.id}/participants") {
            bearerAuth(volunteer.accessToken)
        }
        assertEquals(HttpStatusCode.Conflict, duplicateResponse.status)
        assertEquals("ALREADY_PARTICIPATING", duplicateResponse.errorCode())

        // 3. List participants
        val listResponse = client.get("/api/v1/help-requests/${request.id}/participants") {
            bearerAuth(volunteer.accessToken)
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val list = Json.decodeFromString<List<HelpParticipationResponse>>(listResponse.bodyAsText())
        assertEquals(1, list.size)
        assertEquals(volunteer.user.id, list.first().userId)
    }

    @Test
    fun `volunteer can leave and leaving twice returns 404`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val owner = register("owner2@example.com")
        val volunteer = register("volunteer2@example.com")
        val request = sampleHelpRequest(UUID.fromString(owner.user.id))
        services.helpRequestProvider.requests[request.id] = request

        // Join
        client.post("/api/v1/help-requests/${request.id}/participants") {
            bearerAuth(volunteer.accessToken)
        }

        // Leave
        val leaveResponse = client.delete("/api/v1/help-requests/${request.id}/participants/me") {
            bearerAuth(volunteer.accessToken)
        }
        assertEquals(HttpStatusCode.NoContent, leaveResponse.status)

        // Leave again returns 404
        val secondLeave = client.delete("/api/v1/help-requests/${request.id}/participants/me") {
            bearerAuth(volunteer.accessToken)
        }
        assertEquals(HttpStatusCode.NotFound, secondLeave.status)
        assertEquals("PARTICIPATION_NOT_FOUND", secondLeave.errorCode())
    }

    @Test
    fun `only owner can generate check-in code`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val owner = register("owner3@example.com")
        val bystander = register("bystander@example.com")
        val request = sampleHelpRequest(UUID.fromString(owner.user.id))
        services.helpRequestProvider.requests[request.id] = request

        // Bystander fails
        val bystanderResponse = client.post("/api/v1/help-requests/${request.id}/check-in-code") {
            bearerAuth(bystander.accessToken)
        }
        assertEquals(HttpStatusCode.Forbidden, bystanderResponse.status)
        assertEquals("NOT_HELP_REQUEST_OWNER", bystanderResponse.errorCode())

        // Owner succeeds
        val ownerResponse = client.post("/api/v1/help-requests/${request.id}/check-in-code") {
            bearerAuth(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, ownerResponse.status)
        val codeResponse = Json.decodeFromString<CheckInCodeResponse>(ownerResponse.bodyAsText())
        assertEquals(request.id.toString(), codeResponse.helpRequestId)
        assertEquals(6, codeResponse.code.length)
        assertNotNull(codeResponse.expiresAt)
    }

    @Test
    fun `volunteer check-in succeeds with valid code and coordinates, rejects invalid code or location`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val owner = register("owner4@example.com")
        val volunteer = register("volunteer4@example.com")
        val center = Location(51.9244, 4.4777)
        val request = sampleHelpRequest(UUID.fromString(owner.user.id), location = center)
        services.helpRequestProvider.requests[request.id] = request

        // 1. Volunteer joins
        client.post("/api/v1/help-requests/${request.id}/participants") {
            bearerAuth(volunteer.accessToken)
        }

        // 2. Owner generates code
        val codeRes = client.post("/api/v1/help-requests/${request.id}/check-in-code") {
            bearerAuth(owner.accessToken)
        }
        val generatedCode = Json.decodeFromString<CheckInCodeResponse>(codeRes.bodyAsText()).code

        // 3. Check-in with wrong code returns 400
        val wrongCodeRes = client.post("/api/v1/help-requests/${request.id}/check-in") {
            bearerAuth(volunteer.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"code":"000000","latitude":51.9244,"longitude":4.4777}""")
        }
        assertEquals(HttpStatusCode.BadRequest, wrongCodeRes.status)
        assertEquals("INVALID_CHECK_IN_CODE", wrongCodeRes.errorCode())

        // 4. Check-in with far location (e.g. Amsterdam vs Rotterdam, > 50km away) returns 400
        val farLocationRes = client.post("/api/v1/help-requests/${request.id}/check-in") {
            bearerAuth(volunteer.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"code":"$generatedCode","latitude":52.3676,"longitude":4.9041}""")
        }
        assertEquals(HttpStatusCode.BadRequest, farLocationRes.status)
        assertEquals("LOCATION_TOO_FAR", farLocationRes.errorCode())

        // 5. Check-in with correct code and location (same spot) succeeds
        val successRes = client.post("/api/v1/help-requests/${request.id}/check-in") {
            bearerAuth(volunteer.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"code":"$generatedCode","latitude":51.9244,"longitude":4.4777}""")
        }
        assertEquals(HttpStatusCode.OK, successRes.status)
        val checkInResult = Json.decodeFromString<CheckInResponse>(successRes.bodyAsText())
        assertTrue(checkInResult.success)
        assertNotNull(checkInResult.checkedInAt)
    }
}
