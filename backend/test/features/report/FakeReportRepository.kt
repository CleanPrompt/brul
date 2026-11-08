package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.repository.ReportRepository
import java.util.UUID

/** In-memory [ReportRepository] so service and route tests do not need a database. */
class FakeReportRepository : ReportRepository {
    private val reports = mutableMapOf<UUID, Report>()

    override fun findById(id: UUID): Report? = reports[id]

    /** Same filters and order as the SQL query in `ExposedReportRepository`. */
    override fun findMatching(query: ReportQuery): List<Report> =
        reports.values
            .filter { report ->
                (query.ownerId == null || report.ownerId == query.ownerId) &&
                    (query.type == null || report.type == query.type) &&
                    (query.category == null || report.category == query.category) &&
                    (query.status == null || report.status == query.status) &&
                    (query.text == null || report.title.contains(query.text, ignoreCase = true) ||
                        report.description.contains(query.text, ignoreCase = true))
            }
            .sortedWith(compareByDescending<Report> { it.createdAt }.thenBy { it.id })

    override fun save(report: Report): Report = report.also { reports[it.id] = it }

    override fun update(report: Report): Boolean {
        val existing = reports[report.id] ?: return false
        // Like the real repository: the owner, the creation time and the view count do not change.
        reports[report.id] = report.copy(
            ownerId = existing.ownerId,
            createdAt = existing.createdAt,
            viewCount = existing.viewCount
        )
        return true
    }

    override fun incrementViewCount(id: UUID): Boolean {
        val existing = reports[id] ?: return false
        reports[id] = existing.copy(viewCount = existing.viewCount + 1)
        return true
    }

    override fun delete(id: UUID): Boolean = reports.remove(id) != null
}
