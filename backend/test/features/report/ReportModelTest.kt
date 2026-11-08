package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.shared.error.AppException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The domain rules of a report, without a database. */
class ReportModelTest {

    @Test
    fun `a valid report has the type of its details`() {
        val report = ReportTestData.nuisance()

        assertEquals(ReportType.NUISANCE, report.type)
    }

    @Test
    fun `the category must belong to the type`() {
        val error = assertFailsWith<AppException.BadRequest> {
            ReportTestData.nuisance().copy(category = ReportCategory.THEFT)
        }

        assertTrue("does not belong to NUISANCE" in error.message)
    }

    @Test
    fun `title and description may not be empty or too long`() {
        assertFailsWith<AppException.BadRequest> { ReportTestData.nuisance(title = "  ") }
        assertFailsWith<AppException.BadRequest> { ReportTestData.nuisance(title = "x".repeat(121)) }
        assertFailsWith<AppException.BadRequest> { ReportTestData.nuisance().copy(description = "") }

        ReportTestData.nuisance(title = "x".repeat(120))
    }

    @Test
    fun `each type gets its own extra field and no other`() {
        assertEquals(ReportDetails.Nuisance, ReportDetails.of(ReportType.NUISANCE, null, null))
        assertEquals(ReportDetails.Safety(urgent = true), ReportDetails.of(ReportType.SAFETY, true, null))
        assertEquals(ReportDetails.HelpRequest(3), ReportDetails.of(ReportType.HELP_REQUEST, null, 3))

        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.NUISANCE, true, null) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.SAFETY, null, null) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.SAFETY, false, 2) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.HELP_REQUEST, null, null) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.of(ReportType.HELP_REQUEST, true, 2) }
    }

    @Test
    fun `a help request needs between 1 and 50 volunteers`() {
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(0) }
        assertFailsWith<AppException.BadRequest> { ReportDetails.HelpRequest(51) }

        ReportDetails.HelpRequest(50)
    }

    @Test
    fun `the status only moves forward`() {
        assertTrue(ReportStatus.OPEN.canChangeTo(ReportStatus.IN_PROGRESS))
        assertTrue(ReportStatus.OPEN.canChangeTo(ReportStatus.RESOLVED))
        assertTrue(ReportStatus.IN_PROGRESS.canChangeTo(ReportStatus.RESOLVED))
        assertTrue(ReportStatus.IN_PROGRESS.canChangeTo(ReportStatus.IN_PROGRESS))

        assertFalse(ReportStatus.IN_PROGRESS.canChangeTo(ReportStatus.OPEN))
        assertFalse(ReportStatus.RESOLVED.canChangeTo(ReportStatus.OPEN))
        assertFalse(ReportStatus.RESOLVED.canChangeTo(ReportStatus.IN_PROGRESS))
    }

    @Test
    fun `a location must be on earth`() {
        assertFailsWith<AppException.BadRequest> { Location(90.1, 4.7) }
        assertFailsWith<AppException.BadRequest> { Location(51.5, -180.1) }
    }

    @Test
    fun `the distance between Breda and Tilburg is about 22 km`() {
        val tilburg = Location(51.5555, 5.0913)

        val distance = ReportTestData.BREDA.distanceKmTo(tilburg)

        assertTrue(abs(distance - 22.0) < 1.0, "was $distance")
        assertEquals(0.0, ReportTestData.BREDA.distanceKmTo(ReportTestData.BREDA))
    }
}
