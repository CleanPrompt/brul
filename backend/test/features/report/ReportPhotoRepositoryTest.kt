package nl.brul.backend.features.report

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.report.database.ReportPhotosTable
import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.LocationSource
import nl.brul.backend.features.report.model.PhotoLocation
import nl.brul.backend.features.report.model.ReportPhoto
import nl.brul.backend.features.report.repository.ExposedReportPhotoRepository
import nl.brul.backend.features.report.repository.ExposedReportRepository
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportPhotoRepositoryTest {
    private val repository = ExposedReportPhotoRepository()
    private val reportRepository = ExposedReportRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:report_photo_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, ReportsTable, ReportPhotosTable) }
    }

    /** A new user with a new report, because the foreign keys need both. */
    private fun newReport(): Pair<UUID, UUID> {
        val owner = userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Owner", "hash", true, TEST_CREATED_AT)
        ).id
        return reportRepository.save(ReportTestData.nuisance(owner)).id to owner
    }

    private fun photo(
        reportId: UUID,
        uploaderId: UUID,
        createdAt: Instant = TEST_CREATED_AT,
        location: PhotoLocation? = null
    ): ReportPhoto {
        val id = UUID.randomUUID()
        return ReportPhoto(id, reportId, uploaderId, "image/jpeg", 1_234, "$id.jpg", createdAt, location)
    }

    @Test
    fun `saves and retrieves a photo with and without a location`() {
        val (reportId, owner) = newReport()
        val location = PhotoLocation(Location(51.5856, 4.7763), LocationSource.PHOTO)
        val withLocation = photo(reportId, owner, location = location)
        val withoutLocation = photo(reportId, owner)

        repository.save(withLocation)
        repository.save(withoutLocation)

        assertEquals(withLocation, repository.findById(withLocation.id))
        assertEquals(withoutLocation, repository.findById(withoutLocation.id))
    }

    @Test
    fun `lists the photos of one report, oldest first, and counts them`() {
        val (reportId, owner) = newReport()
        val newer = repository.save(photo(reportId, owner, createdAt = TEST_CREATED_AT.plusSeconds(60)))
        val older = repository.save(photo(reportId, owner, createdAt = TEST_CREATED_AT))
        val (otherReport, otherOwner) = newReport()
        repository.save(photo(otherReport, otherOwner))

        assertEquals(listOf(older, newer), repository.findByReport(reportId))
        assertEquals(2, repository.countByReport(reportId))
    }

    @Test
    fun `delete removes only that photo`() {
        val (reportId, owner) = newReport()
        val kept = repository.save(photo(reportId, owner))
        val removed = repository.save(photo(reportId, owner))

        assertTrue(repository.delete(removed.id))
        assertFalse(repository.delete(removed.id))
        assertNull(repository.findById(removed.id))
        assertEquals(listOf(kept), repository.findByReport(reportId))
    }

    @Test
    fun `deleting the report removes its photo rows (CASCADE)`() {
        val (reportId, owner) = newReport()
        repository.save(photo(reportId, owner))

        reportRepository.delete(reportId)

        assertEquals(0, repository.countByReport(reportId))
    }
}
