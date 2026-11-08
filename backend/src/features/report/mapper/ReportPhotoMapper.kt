package nl.brul.backend.features.report.mapper

import nl.brul.backend.features.report.dto.ReportPhotoResponse
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.ReportPhoto
import nl.brul.backend.shared.error.AppException

/** The JSON for the app. The image itself is not included; the app downloads it from [ReportPhotoResponse.url]. */
fun ReportPhoto.toResponse(): ReportPhotoResponse =
    ReportPhotoResponse(
        id = id.toString(),
        reportId = reportId.toString(),
        uploaderId = uploaderId.toString(),
        contentType = contentType,
        sizeBytes = sizeBytes,
        createdAt = createdAt.toString(),
        latitude = location?.location?.latitude,
        longitude = location?.location?.longitude,
        locationSource = location?.source?.name,
        url = "/api/v1/photos/$id"
    )

/**
 * The optional `latitude` and `longitude` fields of an upload form as a [Location].
 * Both or neither must be sent; anything else is a 400. `Location` itself checks the ranges.
 */
fun appLocationOf(latitude: String?, longitude: String?): Location? {
    if (latitude.isNullOrBlank() && longitude.isNullOrBlank()) {
        return null
    }

    val lat = latitude?.trim()?.toDoubleOrNull()
    val lon = longitude?.trim()?.toDoubleOrNull()

    if (lat == null || lon == null) {
        throw AppException.BadRequest("latitude and longitude must be valid numbers and sent together")
    }

    return Location(lat, lon)
}
