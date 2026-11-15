package nl.brul.backend.config

import nl.brul.backend.features.authorization.database.PermissionsTable
import nl.brul.backend.features.authorization.database.RolePermissionsTable
import nl.brul.backend.features.authorization.database.RolesTable
import nl.brul.backend.features.authorization.database.UserRolesTable
import nl.brul.backend.features.feedback.database.FeedbackTable
import nl.brul.backend.features.help.database.HelpParticipationsTable
import nl.brul.backend.features.help.database.HelpCheckInCodesTable
import nl.brul.backend.features.report.database.ReportsTable
import nl.brul.backend.features.report.database.ReportPhotosTable
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.vote.database.VotesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: AppConfig) {
        Database.connect(
            url = config.databaseUrl,
            driver = config.databaseDriver,
            user = config.databaseUser,
            password = config.databasePassword
        )

        transaction {
            // H2 DDL commits implicitly: reject orphaned legacy data before any schema mutation.
            val existingTables = exec(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = CURRENT_SCHEMA"
            ) { rows -> buildSet { while (rows.next()) add(rows.getString(1)) } } ?: emptySet()
            if ("HELP_PARTICIPATIONS" in existingTables) {
                val orphanQuery = if ("REPORTS" in existingTables) {
                    """SELECT COUNT(*) FROM help_participations p
                       LEFT JOIN reports r ON r.id = p.help_request_id WHERE r.id IS NULL"""
                } else {
                    "SELECT COUNT(*) FROM help_participations"
                }
                val orphanCount = exec(orphanQuery) { it.next(); it.getInt(1) } ?: 0
                check(orphanCount == 0) {
                    "Cannot link Help to Reports: $orphanCount existing participations have no report. Reconcile these records before restarting; no records or schema were changed."
                }
            }
            SchemaUtils.create(
                UsersTable,
                RolesTable,
                PermissionsTable,
                UserRolesTable,
                RolePermissionsTable,
                FeedbackTable,
                VotesTable,
                ReportsTable,
                ReportPhotosTable,
                HelpParticipationsTable,
                HelpCheckInCodesTable
            )
            // Add only the new nullable planning columns; never rewrite existing report data.
            exec("ALTER TABLE reports ADD COLUMN IF NOT EXISTS start_time TIMESTAMP(9) NULL")
            exec("ALTER TABLE reports ADD COLUMN IF NOT EXISTS end_time TIMESTAMP(9) NULL")
            val hasReportForeignKey = exec(
                """SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                   JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t
                     ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA
                   WHERE k.TABLE_NAME = 'HELP_PARTICIPATIONS' AND k.COLUMN_NAME = 'HELP_REQUEST_ID'
                     AND t.CONSTRAINT_TYPE = 'FOREIGN KEY'"""
            ) { it.next(); it.getInt(1) > 0 } ?: false
            if (!hasReportForeignKey) {
                exec("""ALTER TABLE help_participations ADD CONSTRAINT fk_help_participations_report
                    FOREIGN KEY (help_request_id) REFERENCES reports(id) ON DELETE CASCADE""")
            }
        }
    }
}
