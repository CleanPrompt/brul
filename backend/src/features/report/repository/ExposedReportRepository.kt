package nl.brul.backend.features.report.repository

import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.mapper.toReport
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.shared.database.containsIgnoringCase
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedReportRepository : ReportRepository {
    override fun findById(id: UUID): Report? = transaction {
        ReportsTable.selectAll().where { ReportsTable.id eq id }
            .singleOrNull()?.toReport()
    }

    override fun findMatching(query: ReportQuery): List<Report> = transaction {
        filtered(query)
            .orderBy(ReportsTable.createdAt to SortOrder.DESC, ReportsTable.id to SortOrder.ASC)
            .map { it.toReport() }
    }

    /** Starts with every row and adds one `AND` condition for each filter that is set. */
    private fun filtered(query: ReportQuery): Query {
        val rows = ReportsTable.selectAll()
        query.ownerId?.let { ownerId -> rows.andWhere { ReportsTable.ownerId eq ownerId } }
        query.type?.let { type -> rows.andWhere { ReportsTable.type eq type } }
        query.category?.let { category -> rows.andWhere { ReportsTable.category eq category } }
        query.status?.let { status -> rows.andWhere { ReportsTable.status eq status } }
        query.text?.let { text ->
            rows.andWhere {
                ReportsTable.title.containsIgnoringCase(text) or ReportsTable.description.containsIgnoringCase(text)
            }
        }
        return rows
    }

    override fun save(report: Report): Report {
        transaction {
            ReportsTable.insert {
                it[id] = report.id
                it[ownerId] = report.ownerId
                it[type] = report.type
                it[createdAt] = report.createdAt
                it[viewCount] = report.viewCount
                writeContent(it, report)
            }
        }
        return report
    }

    override fun update(report: Report): Boolean = transaction {
        ReportsTable.update({ ReportsTable.id eq report.id }) {
            writeContent(it, report)
        } > 0
    }

    override fun incrementViewCount(id: UUID): Boolean = transaction {
        ReportsTable.update({ ReportsTable.id eq id }) {
            with(SqlExpressionBuilder) {
                it[viewCount] = viewCount + 1
            }
        } > 0
    }

    override fun delete(id: UUID): Boolean = transaction {
        ReportsTable.deleteWhere { ReportsTable.id eq id } > 0
    }

    /** The columns that `save` and `update` both write; the column that does not belong to the type is emptied. */
    private fun writeContent(statement: UpdateBuilder<*>, report: Report) {
        statement[ReportsTable.title] = report.title
        statement[ReportsTable.description] = report.description
        statement[ReportsTable.category] = report.category
        statement[ReportsTable.latitude] = report.location.latitude
        statement[ReportsTable.longitude] = report.location.longitude
        statement[ReportsTable.status] = report.status
        statement[ReportsTable.urgent] = (report.details as? ReportDetails.Safety)?.urgent
        statement[ReportsTable.volunteersNeeded] = (report.details as? ReportDetails.HelpRequest)?.volunteersNeeded
    }
}
