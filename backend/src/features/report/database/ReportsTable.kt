package nl.brul.backend.features.report.database

import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportStatus
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ReportsTable : Table("reports") {
    val id = uuid("id")
    /** Foreign key: every report belongs to an existing user. Users are never deleted, only deactivated. */
    val ownerId = reference("owner_id", UsersTable.id)
    /** Enums are stored as their name (`SAFETY`), so the database stays readable. */
    val type = enumerationByName("type", 20, ReportType::class)
    val title = varchar("title", 120)
    val description = varchar("description", 2000)
    val category = enumerationByName("category", 30, ReportCategory::class)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val status = enumerationByName("status", 20, ReportStatus::class)
    /** Only for SAFETY reports; empty for the other types. */
    val urgent = bool("urgent").nullable()
    /** Only for HELP_REQUEST reports; empty for the other types. */
    val volunteersNeeded = integer("volunteers_needed").nullable()
    val createdAt = timestamp("created_at")
    val viewCount = integer("view_count").default(0)

    override val primaryKey = PrimaryKey(id)
}
