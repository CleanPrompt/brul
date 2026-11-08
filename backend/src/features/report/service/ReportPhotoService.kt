package nl.brul.backend.features.report.service

import nl.brul.backend.features.report.dto.ReportPhotoResponse
import nl.brul.backend.features.report.mapper.toResponse
import nl.brul.backend.features.report.model.ImageFormat
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.LocationSource
import nl.brul.backend.features.report.model.PhotoFile
import nl.brul.backend.features.report.model.PhotoLocation
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportPhoto
import nl.brul.backend.features.report.repository.PhotoStorage
import nl.brul.backend.features.report.repository.ReportPhotoRepository
import nl.brul.backend.features.report.repository.ReportRepository
import nl.brul.backend.shared.error.AppException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Business rules for photos of reports. Access checks happen in the routes, through `AuthorizationService`.
 *
 * An upload: `PhotoRules` checks the photo (P2-P4), the location comes from the photo itself (G1) or else from
 * the app (G2), the file is stored under the photo's id and goes to [storage], and the data goes to the database.
 * [clock] is a parameter so tests can use a fixed time.
 */
class ReportPhotoService(
    private val reportRepository: ReportRepository,
    private val photoRepository: ReportPhotoRepository,
    private val storage: PhotoStorage,
    private val clock: Clock = Clock.systemUTC()
) {
    /** The owner of a report, so a route can check access before it adds a photo. 404 when unknown. */
    fun getReportOwnerId(reportId: UUID): UUID =
        findReport(reportId).ownerId

    /** The uploader of a photo, so a route can check access before it deletes the photo. 404 when unknown. */
    fun getUploaderId(photoId: UUID): UUID =
        findPhoto(photoId).uploaderId

    /** The server sets the id, the file name, the time and the location; the client only sends the photo. */
    fun upload(reportId: UUID, uploaderId: UUID, bytes: ByteArray, appLocation: Location?): ReportPhotoResponse {
        findReport(reportId) // 404 when the report does not exist

        val format = PhotoRules.check(
            sizeBytes = bytes.size.toLong(),
            format = ImageFormat.detect(bytes),
            existingPhotoCount = photoRepository.countByReport(reportId)
        )

        val id = UUID.randomUUID()
        val photo = ReportPhoto(
            id = id,
            reportId = reportId,
            uploaderId = uploaderId,
            contentType = format.contentType,
            sizeBytes = bytes.size.toLong(),
            storageKey = "$id.${format.extension}",
            createdAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS),
            location = locationOf(bytes, format, appLocation)
        )

        storage.save(photo.storageKey, bytes)

        try {
            photoRepository.save(photo)
        } catch (exception: Exception) {
            // Do not leave a file without data behind when saving the data fails.
            storage.delete(photo.storageKey)
            throw exception
        }

        return photo.toResponse()
    }

    /** All photos of a report, oldest first. 404 when the report does not exist. */
    fun listByReport(reportId: UUID): List<ReportPhotoResponse> {
        findReport(reportId)
        return photoRepository.findByReport(reportId).map { it.toResponse() }
    }

    fun getImage(photoId: UUID): PhotoFile {
        val photo = findPhoto(photoId)
        val bytes = storage.load(photo.storageKey) ?: throw photoNotFound()
        return PhotoFile(photo.contentType, bytes)
    }

    fun delete(photoId: UUID) {
        val photo = findPhoto(photoId)

        if (!photoRepository.delete(photoId)) {
            throw photoNotFound()
        }

        storage.delete(photo.storageKey)
    }

    /**
     * The file keys of a report. `ReportService` reads them before it deletes a report, because the database
     * removes the photo rows with it (CASCADE) but not the files.
     */
    fun storageKeysOf(reportId: UUID): List<String> =
        photoRepository.findByReport(reportId).map { it.storageKey }

    /** Removes the files of a deleted report. */
    fun deleteFiles(keys: List<String>) {
        keys.forEach { storage.delete(it) }
    }

    /** G1: the GPS in the photo wins. G2: otherwise the location from the app. Neither: no location. */
    private fun locationOf(bytes: ByteArray, format: ImageFormat, appLocation: Location?): PhotoLocation? {
        val fromPhoto = if (format == ImageFormat.JPEG) PhotoGpsReader.read(bytes) else null

        return when {
            fromPhoto != null -> PhotoLocation(fromPhoto, LocationSource.PHOTO)
            appLocation != null -> PhotoLocation(appLocation, LocationSource.APP)
            else -> null
        }
    }

    private fun findReport(reportId: UUID): Report =
        reportRepository.findById(reportId) ?: throw AppException.NotFound("Report not found", "REPORT_NOT_FOUND")

    private fun findPhoto(photoId: UUID): ReportPhoto =
        photoRepository.findById(photoId) ?: throw photoNotFound()

    private fun photoNotFound() = AppException.NotFound("Photo not found", "PHOTO_NOT_FOUND")
}
