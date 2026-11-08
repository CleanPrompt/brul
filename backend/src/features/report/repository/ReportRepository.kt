package nl.brul.backend.features.report.repository

import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportQuery
import java.util.UUID

interface ReportRepository {
    fun findById(id: UUID): Report?

    /**
     * Every report that matches the text, type, category, status and owner filters of [query], newest first.
     * The area, the sorting and the page are applied by `ReportSearch`, because H2 cannot compute distances.
     */
    fun findMatching(query: ReportQuery): List<Report>

    fun save(report: Report): Report

    /** Updates the content and the status; the owner, the creation time and the view count never change here. */
    fun update(report: Report): Boolean

    /** Adds one to the view count in the database itself, so two readers at the same time are both counted. */
    fun incrementViewCount(id: UUID): Boolean

    /** Deletes the report for good, and its photo rows through the foreign key; `false` when there is none. */
    fun delete(id: UUID): Boolean
}
