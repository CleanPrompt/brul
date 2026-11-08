package nl.brul.backend.features.report.service

import nl.brul.backend.features.report.dto.CreateReportRequest
import nl.brul.backend.features.report.dto.ReportResponse
import nl.brul.backend.features.report.dto.UpdateReportRequest
import nl.brul.backend.features.report.mapper.toReport
import nl.brul.backend.features.report.mapper.toResponse
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.repository.ReportRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.paging.PageResponse
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Business rules for reports. Access checks happen in the routes, through `AuthorizationService`.
 * [photoService] removes the photo files of a deleted report. [clock] is a parameter so tests can use a fixed time.
 */
class ReportService(
    private val repository: ReportRepository,
    private val photoService: ReportPhotoService,
    private val clock: Clock = Clock.systemUTC()
) {
    /** The server sets the id, the owner ([ownerId]), the status `OPEN` and the time. */
    fun create(ownerId: UUID, request: CreateReportRequest): ReportResponse {
        val report = request.toReport(
            id = UUID.randomUUID(),
            ownerId = ownerId,
            createdAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
        )
        return repository.save(report).toResponse()
    }

    /** One page of matching reports, plus the total number of matches. */
    fun search(query: ReportQuery): PageResponse<ReportResponse> {
        val matches = ReportSearch.select(repository.findMatching(query), query)

        return PageResponse(
            items = matches.drop(query.page.offset).take(query.page.limit).map { it.toResponse() },
            total = matches.size.toLong(),
            limit = query.page.limit,
            offset = query.page.offset
        )
    }

    /** Opening a report counts as a view; the answer already includes this view. */
    fun getById(id: UUID): ReportResponse {
        val report = findReport(id)
        repository.incrementViewCount(id)
        return report.copy(viewCount = report.viewCount + 1).toResponse()
    }

    /** The owner of a report, so a route can check access before it changes the report. */
    fun getOwnerId(id: UUID): UUID =
        findReport(id).ownerId

    /** The status rule: only forward, and a resolved report stays resolved (409 otherwise). */
    fun update(id: UUID, request: UpdateReportRequest): ReportResponse {
        val existing = findReport(id)
        val updated = request.toReport(existing)

        if (!existing.status.canChangeTo(updated.status)) {
            throw AppException.Conflict(
                "The status cannot change from ${existing.status} to ${updated.status}",
                "INVALID_STATUS_CHANGE"
            )
        }

        repository.update(updated)
        return updated.toResponse()
    }

    /** A hard delete. The database removes the photo rows (CASCADE); the files are removed here. */
    fun delete(id: UUID) {
        val photoKeys = photoService.storageKeysOf(id)

        if (!repository.delete(id)) {
            throw reportNotFound()
        }

        photoService.deleteFiles(photoKeys)
    }

    private fun findReport(id: UUID): Report =
        repository.findById(id) ?: throw reportNotFound()

    private fun reportNotFound() = AppException.NotFound("Report not found", "REPORT_NOT_FOUND")
}
