package nl.brul.backend.features.report.mapper

import nl.brul.backend.features.report.database.ReportPhotosTable
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.PhotoLocation
import nl.brul.backend.features.report.model.ReportPhoto
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toReportPhoto(): ReportPhoto =
    ReportPhoto(
        id = this[ReportPhotosTable.id],
        reportId = this[ReportPhotosTable.reportId],
        uploaderId = this[ReportPhotosTable.uploaderId],
        contentType = this[ReportPhotosTable.contentType],
        sizeBytes = this[ReportPhotosTable.sizeBytes],
        storageKey = this[ReportPhotosTable.storageKey],
        createdAt = this[ReportPhotosTable.createdAt],
        location = toPhotoLocation()
    )

/** The three location columns are either all filled or all empty. */
private fun ResultRow.toPhotoLocation(): PhotoLocation? {
    val latitude = this[ReportPhotosTable.latitude]
    val longitude = this[ReportPhotosTable.longitude]
    val source = this[ReportPhotosTable.locationSource]

    if (latitude == null || longitude == null || source == null) {
        return null
    }

    return PhotoLocation(Location(latitude, longitude), source)
}
