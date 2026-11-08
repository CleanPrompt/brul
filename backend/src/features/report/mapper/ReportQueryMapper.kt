package nl.brul.backend.features.report.mapper

import io.ktor.http.Parameters
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.model.ReportSortField
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import nl.brul.backend.shared.http.oneOf
import nl.brul.backend.shared.http.page
import nl.brul.backend.shared.http.searchText
import nl.brul.backend.shared.http.sortDirection
import nl.brul.backend.shared.http.uuidOrNull
import nl.brul.backend.shared.paging.SortDirection

/** Largest search area; a neighbourhood app has no use for more. */
const val MAX_RADIUS_KM = 100.0

/**
 * Reads the query parameters of `GET /reports`, e.g.
 * `?q=bike&type=SAFETY&latitude=51.58&longitude=4.77&radiusKm=2&sort=distance&limit=20`. An invalid value gives 400.
 */
fun Parameters.toReportQuery(): ReportQuery {
    val near = location()
    val radiusKm = doubleOrNull("radiusKm")
    val sortField = oneOf("sort", ReportSortField.entries.associateBy { it.key }) ?: ReportSortField.CREATED_AT

    if (radiusKm != null) {
        requireValid(near != null) { "radiusKm needs latitude and longitude" }
        requireValid(radiusKm > 0 && radiusKm <= MAX_RADIUS_KM) {
            "radiusKm must be above 0 and at most $MAX_RADIUS_KM"
        }
    }
    requireValid(sortField != ReportSortField.DISTANCE || near != null) { "sort=distance needs latitude and longitude" }

    return ReportQuery(
        text = searchText("q"),
        type = oneOf("type", ReportType.entries.associateBy { it.name }),
        category = oneOf("category", ReportCategory.entries.associateBy { it.name }),
        status = oneOf("status", ReportStatus.entries.associateBy { it.name }),
        ownerId = uuidOrNull("ownerId"),
        near = near,
        radiusKm = radiusKm,
        sortField = sortField,
        // Newest first is the default, but for distance the nearest report should come first.
        sortDirection = if (sortField == ReportSortField.DISTANCE && this["order"] == null) {
            SortDirection.ASC
        } else {
            sortDirection()
        },
        page = page()
    )
}

/** `latitude` and `longitude` together, or neither. */
private fun Parameters.location(): Location? {
    val latitude = doubleOrNull("latitude")
    val longitude = doubleOrNull("longitude")

    if (latitude == null && longitude == null) {
        return null
    }
    if (latitude == null || longitude == null) {
        throw AppException.BadRequest("latitude and longitude must be sent together")
    }

    return Location(latitude, longitude)
}

private fun Parameters.doubleOrNull(name: String): Double? =
    this[name]?.let { value ->
        value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: throw AppException.BadRequest("$name must be a number")
    }
