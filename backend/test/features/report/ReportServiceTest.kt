package nl.brul.backend.features.report

import nl.brul.backend.features.report.dto.CreateReportRequest
import nl.brul.backend.features.report.dto.UpdateReportRequest
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.service.ReportPhotoService
import nl.brul.backend.features.report.service.ReportService
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.paging.Page
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportServiceTest {
    private val now = Instant.parse("2026-11-08T09:30:00Z")
    private val repository = FakeReportRepository()
    private val photoRepository = FakeReportPhotoRepository()
    private val storage = InMemoryPhotoStorage()
    private val photoService = ReportPhotoService(repository, photoRepository, storage)
    private val service = ReportService(repository, photoService, Clock.fixed(now, ZoneOffset.UTC))
    private val owner = UUID.randomUUID()

    private fun createRequest(
        type: String = "SAFETY",
        category: String = "THEFT",
        urgent: Boolean? = true,
        volunteersNeeded: Int? = null
    ) = CreateReportRequest(
        type = type,
        title = "  Bike stolen  ",
        description = "My bike was stolen at the station",
        category = category,
        latitude = 51.5866,
        longitude = 4.7760,
        urgent = urgent,
        volunteersNeeded = volunteersNeeded
    )

    private fun updateRequest(status: String = "IN_PROGRESS", type: String = "SAFETY") = UpdateReportRequest(
        type = type,
        title = "Bike found",
        description = "The bike is back",
        category = "THEFT",
        latitude = 51.5866,
        longitude = 4.7760,
        status = status,
        urgent = false
    )

    @Test
    fun `create sets the owner, the status OPEN and the time, and trims the text`() {
        val report = service.create(owner, createRequest())

        assertEquals(owner.toString(), report.ownerId)
        assertEquals("OPEN", report.status)
        assertEquals("2026-11-08T09:30:00Z", report.createdAt)
        assertEquals("Bike stolen", report.title)
        assertEquals(true, report.urgent)
        assertNull(report.volunteersNeeded)
        assertEquals(0, report.viewCount)
    }

    @Test
    fun `create rejects an unknown type, a wrong category and a missing extra field`() {
        val unknownType = assertFailsWith<AppException.BadRequest> {
            service.create(owner, createRequest(type = "FIRE"))
        }
        assertTrue("type must be one of" in unknownType.message)

        assertFailsWith<AppException.BadRequest> { service.create(owner, createRequest(category = "NOISE")) }
        assertFailsWith<AppException.BadRequest> { service.create(owner, createRequest(urgent = null)) }
    }

    @Test
    fun `a help request keeps its number of volunteers`() {
        val report = service.create(
            owner,
            createRequest(type = "HELP_REQUEST", category = "GROCERIES", urgent = null, volunteersNeeded = 2)
        )

        assertEquals(2, report.volunteersNeeded)
        assertNull(report.urgent)
    }

    @Test
    fun `search returns one page and the total`() {
        repeat(3) { service.create(owner, createRequest()) }

        val page = service.search(ReportQuery(page = Page(limit = 2, offset = 0)))

        assertEquals(2, page.items.size)
        assertEquals(3, page.total)
    }

    @Test
    fun `opening a report counts as a view`() {
        val id = UUID.fromString(service.create(owner, createRequest()).id)

        assertEquals(1, service.getById(id).viewCount)
        assertEquals(2, service.getById(id).viewCount)
    }

    @Test
    fun `an unknown report gives 404`() {
        val error = assertFailsWith<AppException.NotFound> { service.getById(UUID.randomUUID()) }

        assertEquals("REPORT_NOT_FOUND", error.code)
        assertFailsWith<AppException.NotFound> { service.getOwnerId(UUID.randomUUID()) }
        assertFailsWith<AppException.NotFound> { service.delete(UUID.randomUUID()) }
    }

    @Test
    fun `update changes the content and the status but keeps the owner, time and views`() {
        val id = UUID.fromString(service.create(owner, createRequest()).id)
        service.getById(id)

        val updated = service.update(id, updateRequest())

        assertEquals("Bike found", updated.title)
        assertEquals("IN_PROGRESS", updated.status)
        assertEquals(false, updated.urgent)
        assertEquals(owner.toString(), updated.ownerId)
        assertEquals("2026-11-08T09:30:00Z", updated.createdAt)
        assertEquals(1, updated.viewCount)
    }

    @Test
    fun `the type of a report cannot change`() {
        val id = UUID.fromString(service.create(owner, createRequest()).id)

        assertFailsWith<AppException.BadRequest> { service.update(id, updateRequest(type = "NUISANCE")) }
    }

    @Test
    fun `a resolved report cannot go back, which gives 409`() {
        val id = UUID.fromString(service.create(owner, createRequest()).id)
        service.update(id, updateRequest(status = "RESOLVED"))

        val error = assertFailsWith<AppException.Conflict> { service.update(id, updateRequest(status = "OPEN")) }

        assertEquals("INVALID_STATUS_CHANGE", error.code)
    }

    @Test
    fun `delete removes the report and the files of its photos`() {
        val id = UUID.fromString(service.create(owner, createRequest()).id)
        photoService.upload(id, owner, ReportTestData.jpeg(), appLocation = null)

        service.delete(id)

        assertTrue(storage.files.isEmpty())
        assertFailsWith<AppException.NotFound> { service.getById(id) }
    }
}
