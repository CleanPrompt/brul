package nl.brul.backend.features.report.service

import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.model.ReportSortField
import nl.brul.backend.shared.paging.SortDirection

/**
 * The part of a search that needs the distance between two points: H2 cannot compute that in SQL, so it happens here,
 * on the reports the repository already filtered. A pure function without a database, so it is easy to test.
 */
object ReportSearch {

    /** [reports] within the area of [query], in the requested order; the page is taken by the service. */
    fun select(reports: List<Report>, query: ReportQuery): List<Report> {
        val near = query.near
        val radiusKm = query.radiusKm

        val inArea = if (near != null && radiusKm != null) {
            reports.filter { it.location.distanceKmTo(near) <= radiusKm }
        } else {
            reports
        }

        return inArea.sortedWith(comparator(query.sortField, query.sortDirection, near))
    }

    /** The chosen field first, then the id, so reports with the same value keep a stable order across pages. */
    private fun comparator(field: ReportSortField, direction: SortDirection, near: Location?): Comparator<Report> {
        val byField: Comparator<Report> = when (field) {
            ReportSortField.CREATED_AT -> compareBy { it.createdAt }
            ReportSortField.VIEW_COUNT -> compareBy { it.viewCount }
            ReportSortField.DISTANCE -> {
                val centre = checkNotNull(near) { "Sorting on distance needs a location; the mapper checks this" }
                compareBy { it.location.distanceKmTo(centre) }
            }
        }
        val ordered = if (direction == SortDirection.DESC) byField.reversed() else byField

        return ordered.thenBy { it.id }
    }
}
