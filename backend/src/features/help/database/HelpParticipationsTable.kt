package nl.brul.backend.features.help.database

import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Exposed Table definition for volunteer registrations on help requests.
 */
object HelpParticipationsTable : Table("help_participations") {
    val id = uuid("id")
    val helpRequestId = uuid("help_request_id")
    /** Foreign key: every participation belongs to an existing user. */
    val userId = reference("user_id", UsersTable.id)
    /** Stored as the enum name (`SIGNED_UP`, `ATTENDED`, etc.). */
    val status = enumerationByName("status", 30, ParticipationStatus::class)
    /** Timestamp when the volunteer checked in (UTC), or null if not yet checked in. */
    val checkedInAt = timestamp("checked_in_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        // Enforce that a user cannot join the same help request more than once at the database level.
        uniqueIndex("uq_help_participation_request_user", helpRequestId, userId)
    }
}
