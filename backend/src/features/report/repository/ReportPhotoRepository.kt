package nl.brul.backend.features.report.repository

import nl.brul.backend.features.report.model.ReportPhoto
import java.util.UUID

/** Photo data in the database. The real version is `ExposedReportPhotoRepository`; tests use a fake. */
interface ReportPhotoRepository {
    fun findById(id: UUID): ReportPhoto?

    /** All photos of a report, oldest first. */
    fun findByReport(reportId: UUID): List<ReportPhoto>

    fun countByReport(reportId: UUID): Long

    fun save(photo: ReportPhoto): ReportPhoto

    /** `false` when no photo with [id] exists. */
    fun delete(id: UUID): Boolean
}
