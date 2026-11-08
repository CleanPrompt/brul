package nl.brul.backend.features.report

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.report.database.ReportPhotosTable
import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.features.report.repository.ExposedReportRepository
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportRepositoryTest {
    private val repository = ExposedReportRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:report_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, ReportsTable, ReportPhotosTable) }
    }

    /** The foreign key needs an existing user. */
    private fun newOwner(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Owner", "hash", true, TEST_CREATED_AT)
        ).id

    private fun safety(ownerId: UUID): Report =
        ReportTestData.nuisance(ownerId).copy(
            title = "Bike stolen",
            description = "Taken from the rack at the station",
            category = ReportCategory.THEFT,
            details = ReportDetails.Safety(urgent = true)
        )

    private fun helpRequest(ownerId: UUID): Report =
        ReportTestData.nuisance(ownerId).copy(
            title = "Help with groceries",
            category = ReportCategory.GROCERIES,
            details = ReportDetails.HelpRequest(volunteersNeeded = 2)
        )

    @Test
    fun `saves and retrieves every type with its extra field`() {
        val owner = newOwner()
        val reports = listOf(ReportTestData.nuisance(owner), safety(owner), helpRequest(owner))

        reports.forEach { repository.save(it) }

        reports.forEach { assertEquals(it, repository.findById(it.id)) }
    }

    // The in-memory database is shared by the tests in this class, so every search test filters on a new owner.

    @Test
    fun `findMatching filters on the owner and returns the newest first`() {
        val owner = newOwner()
        val older = repository.save(ReportTestData.nuisance(owner, createdAt = TEST_CREATED_AT))
        val newer = repository.save(ReportTestData.nuisance(owner, createdAt = TEST_CREATED_AT.plusSeconds(60)))
        repository.save(ReportTestData.nuisance(newOwner()))

        assertEquals(listOf(newer, older), repository.findMatching(ReportQuery(ownerId = owner)))
    }

    @Test
    fun `findMatching filters on type, category, status and text in title or description`() {
        val owner = newOwner()
        val graffiti = repository.save(ReportTestData.nuisance(owner))
        val bike = repository.save(safety(owner))

        assertEquals(listOf(bike), repository.findMatching(ReportQuery(ownerId = owner, type = ReportType.SAFETY)))
        assertEquals(
            listOf(graffiti),
            repository.findMatching(ReportQuery(ownerId = owner, category = ReportCategory.VANDALISM))
        )
        assertEquals(listOf(bike), repository.findMatching(ReportQuery(ownerId = owner, text = "BIKE")))
        assertEquals(listOf(graffiti), repository.findMatching(ReportQuery(ownerId = owner, text = "pedestrian")))
        assertEquals(
            emptyList(),
            repository.findMatching(ReportQuery(ownerId = owner, status = ReportStatus.RESOLVED))
        )
    }

    @Test
    fun `update changes the content and the status, but not the owner, time or views`() {
        val report = repository.save(ReportTestData.nuisance(newOwner()))

        val updated = repository.update(
            report.copy(title = "Cleaned", status = ReportStatus.RESOLVED, ownerId = newOwner(), viewCount = 99)
        )

        assertTrue(updated)
        assertEquals(report.copy(title = "Cleaned", status = ReportStatus.RESOLVED), repository.findById(report.id))
    }

    @Test
    fun `the view count goes up by one in the database`() {
        val report = repository.save(ReportTestData.nuisance(newOwner()))

        assertTrue(repository.incrementViewCount(report.id))
        assertTrue(repository.incrementViewCount(report.id))

        assertEquals(2, repository.findById(report.id)?.viewCount)
        assertFalse(repository.incrementViewCount(UUID.randomUUID()))
    }

    @Test
    fun `delete removes the row and reports a missing row`() {
        val report = repository.save(ReportTestData.nuisance(newOwner()))

        assertTrue(repository.delete(report.id))
        assertNull(repository.findById(report.id))
        assertFalse(repository.delete(report.id))
    }

    @Test
    fun `rejects a report for a user that does not exist`() {
        assertFailsWith<ExposedSQLException> { repository.save(ReportTestData.nuisance(UUID.randomUUID())) }
    }
}
