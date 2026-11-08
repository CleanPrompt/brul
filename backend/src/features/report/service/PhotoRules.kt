package nl.brul.backend.features.report.service

import nl.brul.backend.features.report.model.ImageFormat
import nl.brul.backend.shared.error.AppException

/**
 * The rules for the photo itself, as one pure function without a database (P2 to P4).
 * P1, who may add photos, is the access check in the route (`AuthorizationService.requireOwnerOrPermission`).
 *
 * The checks run in a fixed order; the first one that fails decides the error.
 */
object PhotoRules {

    /** P2: at most 5 MB per photo. */
    const val MAX_PHOTO_BYTES: Long = 5L * 1024 * 1024

    /** P4: at most 3 photos per report. */
    const val MAX_PHOTOS_PER_REPORT = 3

    /** Returns the format of an allowed photo, or throws the [AppException] of the first broken rule. */
    fun check(sizeBytes: Long, format: ImageFormat?, existingPhotoCount: Long): ImageFormat {
        if (sizeBytes > MAX_PHOTO_BYTES) {
            throw tooLarge()
        }

        if (format == null) {
            throw AppException.UnsupportedMediaType(
                "Only JPEG and PNG photos are accepted",
                "UNSUPPORTED_PHOTO_TYPE"
            )
        }

        if (existingPhotoCount >= MAX_PHOTOS_PER_REPORT) {
            throw AppException.Conflict(
                "A report can have at most $MAX_PHOTOS_PER_REPORT photos",
                "PHOTO_LIMIT_REACHED"
            )
        }

        return format
    }

    /** P2 as an error; also used by the route when the request itself is already too large. */
    fun tooLarge(): AppException =
        AppException.PayloadTooLarge(
            "A photo can be at most ${MAX_PHOTO_BYTES / (1024 * 1024)} MB",
            "PHOTO_TOO_LARGE"
        )
}
