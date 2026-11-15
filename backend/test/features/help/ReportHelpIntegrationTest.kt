package nl.brul.backend.features.help

import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.*
import nl.brul.backend.config.DatabaseFactory
import nl.brul.backend.features.help.repository.ExposedHelpParticipationRepository
import nl.brul.backend.features.help.service.*
import nl.brul.backend.features.report.ReportTestData
import nl.brul.backend.features.report.dto.ReportResponse
import nl.brul.backend.features.report.model.*
import nl.brul.backend.features.report.mapper.toReport
import nl.brul.backend.features.report.repository.ExposedReportRepository
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Real report rows and production provider, including API wiring and concurrent database requests. */
class ReportHelpIntegrationTest {
    private val reports = ExposedReportRepository()
    private val participations = ExposedHelpParticipationRepository()
    private val provider = ReportHelpRequestProvider(reports)
    private val service = HelpParticipationService(participations, provider)

    private fun config() = testConfig().copy(databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")

    private fun user(): UUID = ExposedUserRepository().save(
        User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Neighbour", "hash", true, TEST_CREATED_AT)
    ).id

    private fun report(capacity: Int = 1, planned: Boolean = false): Report {
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        return reports.save(ReportTestData.nuisance(ownerId = user()).copy(
            category = ReportCategory.CHORES,
            details = ReportDetails.HelpRequest(capacity,
                if (planned) now.minusSeconds(60) else null,
                if (planned) now.plusSeconds(3600) else null)
        ))
    }

    @Test
    fun `API creates a real help report and a volunteer joins without planning`() = testApplication {
        val config = config()
        DatabaseFactory.init(config)
        application { module(config) }
        val owner = register("owner@example.com")
        val volunteer = register("volunteer@example.com")
        val created = client.post("/api/v1/reports") {
            bearerAuth(owner.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"type":"HELP_REQUEST","title":"Help","description":"Groceries","category":"GROCERIES","latitude":51.58,"longitude":4.77,"volunteersNeeded":1}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val report = Json.decodeFromString<ReportResponse>(created.bodyAsText())
        val path = "/api/v1/help-requests/${report.id}"
        assertEquals(HttpStatusCode.Unauthorized, client.post("$path/participants").status)
        assertEquals(HttpStatusCode.Forbidden, client.post("$path/participants") { bearerAuth(owner.accessToken) }.status)
        assertEquals(HttpStatusCode.Created, client.post("$path/participants") { bearerAuth(volunteer.accessToken) }.status)
        assertEquals(HttpStatusCode.Conflict, client.post("$path/participants") { bearerAuth(volunteer.accessToken) }.status)
        assertEquals(HttpStatusCode.OK, client.get("$path/participants/summary").status)
        val calendar = client.get("$path/calendar.ics")
        assertEquals(HttpStatusCode.Conflict, calendar.status)
        assertEquals("HELP_REQUEST_NOT_SCHEDULED", calendar.errorCode())
        assertEquals(0, reports.findById(UUID.fromString(report.id))!!.viewCount)
    }

    @Test
    fun `only real OPEN help requests accept volunteers`() {
        DatabaseFactory.init(config())
        val request = report()
        val volunteer = user()
        assertFailsWith<AppException.Forbidden> { service.join(request.id, request.ownerId) }
        val nuisance = reports.save(ReportTestData.nuisance(ownerId = user()))
        assertFailsWith<AppException.NotFound> { service.join(nuisance.id, volunteer) }
        assertFailsWith<AppException.NotFound> { service.join(UUID.randomUUID(), volunteer) }
        for (status in listOf(ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED)) {
            reports.update(request.copy(status = status))
            assertEquals("HELP_REQUEST_CLOSED", assertFailsWith<AppException.Conflict> { service.join(request.id, volunteer) }.code)
        }
    }

    @Test
    fun `simultaneous volunteers cannot overbook the final place`() {
        DatabaseFactory.init(config())
        val request = report()
        val users = List(6) { user() }
        val barrier = CyclicBarrier(users.size)
        val pool = Executors.newFixedThreadPool(users.size)
        try {
            val jobs = users.map { id -> pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { service.join(request.id, id); "JOINED" }
                catch (e: AppException.Conflict) { e.code }
            }) }
            val results = jobs.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it == "JOINED" })
            assertEquals(5, results.count { it == "HELP_REQUEST_FULL" })
            assertEquals(1, participations.countByHelpRequest(request.id))
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `simultaneous duplicate joins produce a single participation`() {
        DatabaseFactory.init(config())
        val request = report(3)
        val volunteer = user()
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = List(2) { pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { service.join(request.id, volunteer); "JOINED" }
                catch (e: AppException.Conflict) { e.code }
            }) }
            assertEquals(setOf("JOINED", "ALREADY_PARTICIPATING"), jobs.map { it.get(20, TimeUnit.SECONDS) }.toSet())
            assertEquals(1, participations.countByHelpRequest(request.id))
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `capacity cannot be reduced below existing participation count`() {
        DatabaseFactory.init(config())
        val request = report(2)
        service.join(request.id, user())
        service.join(request.id, user())
        assertEquals("HELP_CAPACITY_TOO_LOW", assertFailsWith<AppException.Conflict> {
            reports.update(request.copy(details = ReportDetails.HelpRequest(1)))
        }.code)
        assertEquals(2, provider.findHelpRequest(request.id)!!.volunteersNeeded)
    }

    @Test
    fun `planned report supports calendar persistent codes and GPS check-in`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        val volunteer = user()
        service.join(request.id, volunteer)
        val code = service.generateCheckInCode(request.id, request.ownerId).code
        val restartedProvider = ReportHelpRequestProvider(reports)
        assertEquals(code, restartedProvider.getCheckInCode(request.id)!!.code)
        val calendar = service.getCalendarFeed(request.id)
        assertTrue(calendar.contains("BEGIN:VEVENT"))
        assertTrue(calendar.contains("DTSTART:"))
        assertEquals("LOCATION_TOO_FAR", assertFailsWith<AppException.BadRequest> {
            service.checkIn(request.id, volunteer, code, nl.brul.backend.features.help.model.Location(0.0, 0.0))
        }.code)
        assertTrue(service.checkIn(request.id, volunteer, code,
            nl.brul.backend.features.help.model.Location(request.location.latitude, request.location.longitude)).success)
    }

    @Test
    fun `GPS remains optional for check-in`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        val volunteer = user()
        service.join(request.id, volunteer)
        val code = service.generateCheckInCode(request.id, request.ownerId).code
        assertTrue(service.checkIn(request.id, volunteer, code, null).success)
    }

    @Test
    fun `deleting report cascades to participation and check-in codes`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        service.join(request.id, user())
        service.generateCheckInCode(request.id, request.ownerId)
        val other = report(planned = true)
        val otherParticipation = service.join(other.id, user())
        val otherCode = service.generateCheckInCode(other.id, other.ownerId).code
        reports.delete(request.id)
        assertEquals(otherParticipation, participations.findById(otherParticipation.id))
        assertEquals(otherCode, provider.getCheckInCode(other.id)!!.code)
        assertNotNull(reports.findById(other.id))
        assertEquals(0, participations.countByHelpRequest(request.id))
        assertNull(provider.getCheckInCode(request.id))
        assertNull(provider.findHelpRequest(request.id))
    }

    @Test
    fun `unplanned report rejects calendar and code without inventing dates`() {
        DatabaseFactory.init(config())
        val request = report()
        assertNull(provider.findHelpRequest(request.id)!!.startTime)
        assertEquals("HELP_REQUEST_NOT_SCHEDULED", assertFailsWith<AppException.Conflict> { service.getCalendarFeed(request.id) }.code)
        assertEquals("HELP_REQUEST_NOT_SCHEDULED", assertFailsWith<AppException.Conflict> {
            service.generateCheckInCode(request.id, request.ownerId)
        }.code)
    }

    @Test
    fun `planning is paired ordered and exclusive to help requests`() {
        val start = Instant.parse("2026-11-15T12:00:00Z")
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(1, start, null) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(1, null, start) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(1, start, start) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(1, start, start.minusSeconds(1)) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.NUISANCE, null, null, start, start.plusSeconds(60)) }
    }
    @Test
    fun `planned API report exposes dates calendar and rejects partial GPS`() = testApplication {
        val config = config()
        DatabaseFactory.init(config)
        application { module(config) }
        val owner = register("owner@example.com")
        val volunteer = register("volunteer@example.com")
        val start = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val end = start.plusSeconds(3600)
        val created = client.post("/api/v1/reports") {
            bearerAuth(owner.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"type":"HELP_REQUEST","title":"Help","description":"Groceries","category":"GROCERIES","latitude":51.58,"longitude":4.77,"volunteersNeeded":2,"startTime":"$start","endTime":"$end"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val report = Json.decodeFromString<ReportResponse>(created.bodyAsText())
        assertEquals(start.toString(), report.startTime)
        assertEquals(end.toString(), report.endTime)
        val path = "/api/v1/help-requests/${report.id}"
        assertEquals(HttpStatusCode.OK, client.get("$path/calendar.ics").status)
        assertEquals(HttpStatusCode.Created, client.post("$path/participants") { bearerAuth(volunteer.accessToken) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("$path/check-in-code") { bearerAuth(volunteer.accessToken) }.status)
        val codeResponse = client.post("$path/check-in-code") { bearerAuth(owner.accessToken) }
        assertEquals(HttpStatusCode.OK, codeResponse.status)
        val code = Json.decodeFromString<nl.brul.backend.features.help.dto.CheckInCodeResponse>(codeResponse.bodyAsText()).code
        for (coordinates in listOf("\"latitude\":51.58", "\"latitude\":91,\"longitude\":4.77")) {
            val invalid = client.post("$path/check-in") {
                bearerAuth(volunteer.accessToken)
                contentType(ContentType.Application.Json)
                setBody("""{"code":"$code",$coordinates}""")
            }
            assertEquals(HttpStatusCode.BadRequest, invalid.status)
        }
        assertEquals(HttpStatusCode.OK, client.post("$path/check-in") {
            bearerAuth(volunteer.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"code":"$code"}""")
        }.status)
    }

    @Test
    fun `migration retains reports and adds nullable planning to existing database`() {
        val config = config()
        DatabaseFactory.init(config)
        val request = report()
        org.jetbrains.exposed.sql.transactions.transaction {
            exec("ALTER TABLE reports DROP COLUMN start_time")
            exec("ALTER TABLE reports DROP COLUMN end_time")
        }
        DatabaseFactory.init(config)
        assertEquals(request, reports.findById(request.id))
        DatabaseFactory.init(config) // repeatable
        assertEquals(request, reports.findById(request.id))
    }

    @Test
    fun `capacity update and concurrent join cannot violate the final capacity`() {
        DatabaseFactory.init(config())
        val request = report(2)
        service.join(request.id, user())
        val volunteer = user()
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val join = pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { service.join(request.id, volunteer); "JOINED" }
                catch (e: AppException.Conflict) { e.code }
            })
            val resize = pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { reports.update(request.copy(details = ReportDetails.HelpRequest(1))); "UPDATED" }
                catch (e: AppException.Conflict) { e.code }
            })
            val outcomes = setOf(join.get(20, TimeUnit.SECONDS), resize.get(20, TimeUnit.SECONDS))
            assertTrue(outcomes == setOf("JOINED", "HELP_CAPACITY_TOO_LOW") || outcomes == setOf("HELP_REQUEST_FULL", "UPDATED"))
            assertTrue(participations.countByHelpRequest(request.id) <= provider.findHelpRequest(request.id)!!.volunteersNeeded)
        } finally { pool.shutdownNow() }
    }

    private fun removeReportForeignKey() = org.jetbrains.exposed.sql.transactions.transaction {
        val constraint = exec("""SELECT k.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
            JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t
            ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA
            WHERE k.TABLE_NAME = 'HELP_PARTICIPATIONS' AND k.COLUMN_NAME = 'HELP_REQUEST_ID'
            AND t.CONSTRAINT_TYPE = 'FOREIGN KEY'""") { rows ->
            if (rows.next()) rows.getString(1) else error("Missing test foreign key")
        }
        exec("ALTER TABLE help_participations DROP CONSTRAINT $constraint")
    }

    @Test
    fun `migration links existing valid participations without losing data`() {
        val config = config()
        DatabaseFactory.init(config)
        val request = report()
        val participation = service.join(request.id, user())
        removeReportForeignKey()
        DatabaseFactory.init(config)
        assertEquals(participation, participations.findById(participation.id))
        reports.delete(request.id)
        assertNull(participations.findById(participation.id))
    }

    @Test
    fun `migration refuses orphaned participation without deleting it`() {
        val config = config()
        DatabaseFactory.init(config)
        removeReportForeignKey()
        val orphan = participations.save(nl.brul.backend.features.help.model.HelpParticipation(
            UUID.randomUUID(), UUID.randomUUID(), user()))
        val error = assertFailsWith<IllegalStateException> { DatabaseFactory.init(config) }
        assertTrue(error.message!!.contains("existing participations have no report"))
        assertEquals(orphan, participations.findById(orphan.id))
    }

    @Test
    fun `report update round trips planning and malformed planning gives bad request`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        val details = request.details as ReportDetails.HelpRequest
        val update = nl.brul.backend.features.report.dto.UpdateReportRequest(
            type = "HELP_REQUEST", title = request.title, description = request.description,
            category = request.category.name, latitude = request.location.latitude,
            longitude = request.location.longitude, status = "IN_PROGRESS", volunteersNeeded = 2,
            startTime = details.startTime.toString(), endTime = details.endTime.toString())
        val updated = with(update) { toReportForTest(request) }
        reports.update(updated)
        assertEquals(updated, reports.findById(request.id))
        val cleared = update.copy(startTime = null, endTime = null).toReportForTest(updated)
        val clearedDetails = assertIs<ReportDetails.HelpRequest>(cleared.details)
        assertNull(clearedDetails.startTime)
        assertNull(clearedDetails.endTime)
        assertFailsWith<AppException.BadRequest> { update.copy(startTime = "invalid").toReportForTest(request) }
    }

    private fun nl.brul.backend.features.report.dto.UpdateReportRequest.toReportForTest(report: Report): Report = toReport(report)

    @Test
    fun `stale update cannot reopen a resolved report and allow new volunteers`() {
        DatabaseFactory.init(config())
        val stale = report()
        reports.update(stale.copy(status = ReportStatus.RESOLVED))
        assertEquals("INVALID_STATUS_CHANGE", assertFailsWith<AppException.Conflict> {
            reports.update(stale.copy(title = "Edited from stale client"))
        }.code)
        assertEquals("HELP_REQUEST_CLOSED", assertFailsWith<AppException.Conflict> {
            service.join(stale.id, user())
        }.code)
    }

    @Test
    fun `unplanned volunteer check-in consistently reports missing planning`() {
        DatabaseFactory.init(config())
        val request = report()
        val volunteer = user()
        service.join(request.id, volunteer)
        assertEquals("HELP_REQUEST_NOT_SCHEDULED", assertFailsWith<AppException.Conflict> {
            service.checkIn(request.id, volunteer, "123456", null)
        }.code)
    }

    @Test
    fun `failed attendance update never returns successful check-in`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        val volunteer = user()
        service.join(request.id, volunteer)
        val code = service.generateCheckInCode(request.id, request.ownerId).code
        val noUpdate = object : nl.brul.backend.features.help.repository.HelpParticipationRepository by participations {
            override fun updateStatus(helpRequestId: UUID, userId: UUID,
                status: nl.brul.backend.features.help.model.ParticipationStatus, checkedInAt: Instant?): Boolean = false
        }
        assertEquals("NOT_REGISTERED", assertFailsWith<AppException.NotFound> {
            HelpParticipationService(noUpdate, provider).checkIn(request.id, volunteer, code, null)
        }.code)
    }

    @Test
    fun `orphan preflight stops before adding planning columns`() {
        val config = config()
        DatabaseFactory.init(config)
        removeReportForeignKey()
        val orphan = participations.save(nl.brul.backend.features.help.model.HelpParticipation(
            UUID.randomUUID(), UUID.randomUUID(), user()))
        org.jetbrains.exposed.sql.transactions.transaction {
            exec("ALTER TABLE reports DROP COLUMN start_time")
            exec("ALTER TABLE reports DROP COLUMN end_time")
        }
        assertFailsWith<IllegalStateException> { DatabaseFactory.init(config) }
        val planningColumns = org.jetbrains.exposed.sql.transactions.transaction {
            exec("""SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = CURRENT_SCHEMA
                AND TABLE_NAME = 'REPORTS' AND COLUMN_NAME IN ('START_TIME', 'END_TIME')""") {
                it.next(); it.getInt(1)
            }
        }
        assertEquals(0, planningColumns)
        assertEquals(orphan, participations.findById(orphan.id))
    }

    @Test
    fun `simultaneous check-ins register attendance only once`() {
        DatabaseFactory.init(config())
        val request = report(planned = true)
        val volunteer = user()
        service.join(request.id, volunteer)
        val code = service.generateCheckInCode(request.id, request.ownerId).code
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = List(2) { pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { service.checkIn(request.id, volunteer, code, null); "CHECKED_IN" }
                catch (e: AppException.BadRequest) { e.code }
            }) }
            assertEquals(setOf("CHECKED_IN", "ALREADY_ATTENDED"), jobs.map { it.get(20, TimeUnit.SECONDS) }.toSet())
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `closing and joining serialize and later joins remain rejected`() {
        DatabaseFactory.init(config())
        val request = report(2)
        val volunteer = user()
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val join = pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                try { service.join(request.id, volunteer); "JOINED" }
                catch (e: AppException.Conflict) { e.code }
            })
            val close = pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                reports.update(request.copy(status = ReportStatus.RESOLVED))
            })
            assertTrue(join.get(20, TimeUnit.SECONDS) in setOf("JOINED", "HELP_REQUEST_CLOSED"))
            assertTrue(close.get(20, TimeUnit.SECONDS))
            assertEquals("HELP_REQUEST_CLOSED", assertFailsWith<AppException.Conflict> {
                service.join(request.id, user())
            }.code)
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `report disappearing during update returns not found instead of success`() {
        DatabaseFactory.init(config())
        val request = report()
        val missingOnUpdate = object : nl.brul.backend.features.report.repository.ReportRepository by reports {
            override fun update(report: Report): Boolean = false
        }
        val photos = nl.brul.backend.features.report.service.ReportPhotoService(reports,
            nl.brul.backend.features.report.repository.ExposedReportPhotoRepository(),
            nl.brul.backend.features.report.InMemoryPhotoStorage())
        val reportService = nl.brul.backend.features.report.service.ReportService(missingOnUpdate, photos)
        val update = nl.brul.backend.features.report.dto.UpdateReportRequest(
            "HELP_REQUEST", request.title, request.description, request.category.name,
            request.location.latitude, request.location.longitude, "OPEN", volunteersNeeded = 1)
        assertEquals("REPORT_NOT_FOUND", assertFailsWith<AppException.NotFound> {
            reportService.update(request.id, update)
        }.code)
    }

}
