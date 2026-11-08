package nl.brul.backend.features.report.mapper

import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportType
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toReport(): Report =
    Report(
        id = this[ReportsTable.id],
        ownerId = this[ReportsTable.ownerId],
        title = this[ReportsTable.title],
        description = this[ReportsTable.description],
        category = this[ReportsTable.category],
        location = Location(this[ReportsTable.latitude], this[ReportsTable.longitude]),
        details = toDetails(),
        status = this[ReportsTable.status],
        createdAt = this[ReportsTable.createdAt],
        viewCount = this[ReportsTable.viewCount]
    )

/** The repository always fills the column of the type, so an empty one is a bug and gives a 500. */
private fun ResultRow.toDetails(): ReportDetails =
    when (this[ReportsTable.type]) {
        ReportType.NUISANCE -> ReportDetails.Nuisance
        ReportType.SAFETY -> ReportDetails.Safety(
            this[ReportsTable.urgent] ?: error("Safety report without urgent")
        )
        ReportType.HELP_REQUEST -> ReportDetails.HelpRequest(
            this[ReportsTable.volunteersNeeded] ?: error("Help request without volunteersNeeded")
        )
    }
