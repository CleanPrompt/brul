package nl.brul.backend.features.report.model

import java.time.Instant
import java.util.UUID

/**
 * The data of one photo of a report. The image itself is not in the database but in `PhotoStorage`,
 * under [storageKey].
 */
data class ReportPhoto(
    val id: UUID,
    val reportId: UUID,
    /** The user who added the photo. Users are never deleted, only deactivated. */
    val uploaderId: UUID,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    /** Set by the server when the photo is added (UTC, whole seconds); never changed afterwards. */
    val createdAt: Instant,
    /** Where the photo was taken, or `null` when neither the photo nor the app gave a location. */
    val location: PhotoLocation?
)
