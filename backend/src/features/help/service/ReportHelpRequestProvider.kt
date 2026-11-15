package nl.brul.backend.features.help.service

import nl.brul.backend.features.help.database.HelpCheckInCodesTable
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.repository.ReportRepository
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

/** Reads current report data; codes survive application restarts. */
class ReportHelpRequestProvider(private val reports: ReportRepository) : HelpRequestProvider {
    override fun <T> withRequestLock(id: UUID, action: () -> T): T = transaction {
        ReportsTable.selectAll().where { ReportsTable.id eq id }.forUpdate().singleOrNull()
        action()
    }

    override fun findHelpRequest(id: UUID): HelpRequestInfo? {
        val report = reports.findById(id) ?: return null
        val details = report.details as? ReportDetails.HelpRequest ?: return null
        return HelpRequestInfo(report.id, report.ownerId, report.title, report.description,
            report.category.name, Location(report.location.latitude, report.location.longitude),
            details.volunteersNeeded, report.status == ReportStatus.OPEN, details.startTime, details.endTime)
    }

    override fun setCheckInCode(helpRequestId: UUID, code: String, expiresAt: Instant) {
        withRequestLock(helpRequestId) {
            HelpCheckInCodesTable.deleteWhere { reportId eq helpRequestId }
            HelpCheckInCodesTable.insert {
                it[reportId] = helpRequestId
                it[HelpCheckInCodesTable.code] = code
                it[HelpCheckInCodesTable.expiresAt] = expiresAt
            }
        }
    }

    override fun getCheckInCode(helpRequestId: UUID): ActiveCheckInCode? = transaction {
        HelpCheckInCodesTable.selectAll().where { HelpCheckInCodesTable.reportId eq helpRequestId }
            .singleOrNull()?.let { ActiveCheckInCode(it[HelpCheckInCodesTable.code], it[HelpCheckInCodesTable.expiresAt]) }
    }
}
