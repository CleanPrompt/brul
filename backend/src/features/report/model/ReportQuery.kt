package nl.brul.backend.features.report.model

import nl.brul.backend.shared.paging.Page
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/**
 * What `GET /reports` asks for: filters, an optional area, sorting and a page. A filter that is `null` is not
 * applied. The defaults give the newest reports first.
 */
data class ReportQuery(
    /** Matches part of the title or the description, ignoring case. */
    val text: String? = null,
    val type: ReportType? = null,
    val category: ReportCategory? = null,
    val status: ReportStatus? = null,
    val ownerId: UUID? = null,
    /** The centre for [radiusKm] and for sorting on distance. */
    val near: Location? = null,
    /** Only reports within this many kilometres of [near]. */
    val radiusKm: Double? = null,
    val sortField: ReportSortField = ReportSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val page: Page = Page()
)
