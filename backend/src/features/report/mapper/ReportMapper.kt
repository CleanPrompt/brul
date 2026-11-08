package nl.brul.backend.features.report.mapper

import nl.brul.backend.features.report.dto.CreateReportRequest
import nl.brul.backend.features.report.dto.ReportResponse
import nl.brul.backend.features.report.dto.UpdateReportRequest
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import java.time.Instant
import java.util.UUID

fun Report.toResponse(): ReportResponse =
    ReportResponse(
        id = id.toString(),
        ownerId = ownerId.toString(),
        type = type.name,
        title = title,
        description = description,
        category = category.name,
        latitude = location.latitude,
        longitude = location.longitude,
        status = status.name,
        urgent = (details as? ReportDetails.Safety)?.urgent,
        volunteersNeeded = (details as? ReportDetails.HelpRequest)?.volunteersNeeded,
        createdAt = createdAt.toString(),
        viewCount = viewCount
    )

/** A new report from the request; the service gives it its [id], [ownerId] and [createdAt]. */
fun CreateReportRequest.toReport(id: UUID, ownerId: UUID, createdAt: Instant): Report {
    val type = parseEnum<ReportType>(type, "type")

    return Report(
        id = id,
        ownerId = ownerId,
        title = title.trim(),
        description = description.trim(),
        category = parseEnum<ReportCategory>(category, "category"),
        location = Location(latitude, longitude),
        details = ReportDetails.of(type, urgent, volunteersNeeded),
        status = ReportStatus.OPEN,
        createdAt = createdAt,
        viewCount = 0
    )
}

/**
 * [existing] with the fields of the request. The id, owner, creation time and view count stay the same, and the type
 * cannot change. Whether the new status is allowed is a rule for the service.
 */
fun UpdateReportRequest.toReport(existing: Report): Report {
    val type = parseEnum<ReportType>(type, "type")
    requireValid(type == existing.type) { "The type of a report cannot change" }

    return existing.copy(
        title = title.trim(),
        description = description.trim(),
        category = parseEnum<ReportCategory>(category, "category"),
        location = Location(latitude, longitude),
        details = ReportDetails.of(type, urgent, volunteersNeeded),
        status = parseEnum<ReportStatus>(status, "status")
    )
}

/** Text such as `"SAFETY"` as an enum value, or 400 that lists the allowed values. */
private inline fun <reified T : Enum<T>> parseEnum(value: String, field: String): T =
    enumValues<T>().find { it.name == value }
        ?: throw AppException.BadRequest("$field must be one of: ${enumValues<T>().joinToString()}")
