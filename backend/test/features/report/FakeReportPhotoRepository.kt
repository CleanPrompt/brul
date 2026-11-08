package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.ReportPhoto
import nl.brul.backend.features.report.repository.ReportPhotoRepository
import java.util.UUID

/** In-memory [ReportPhotoRepository]. Set [failOnSave] to simulate a database error. */
class FakeReportPhotoRepository : ReportPhotoRepository {
    val photos = mutableListOf<ReportPhoto>()
    var failOnSave = false

    override fun findById(id: UUID): ReportPhoto? = photos.firstOrNull { it.id == id }

    /** Oldest first, like the real repository; the list keeps the order in which photos were saved. */
    override fun findByReport(reportId: UUID): List<ReportPhoto> = photos.filter { it.reportId == reportId }

    override fun countByReport(reportId: UUID): Long = findByReport(reportId).size.toLong()

    override fun save(photo: ReportPhoto): ReportPhoto {
        check(!failOnSave) { "Database is down" }
        photos += photo
        return photo
    }

    override fun delete(id: UUID): Boolean = photos.removeIf { it.id == id }
}
