package nl.brul.backend.features.help.database

import nl.brul.backend.features.report.database.ReportsTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object HelpCheckInCodesTable : Table("help_check_in_codes") {
    val reportId = reference("report_id", ReportsTable.id, onDelete = ReferenceOption.CASCADE)
    val code = varchar("code", 6)
    val expiresAt = timestamp("expires_at")
    override val primaryKey = PrimaryKey(reportId)
}
